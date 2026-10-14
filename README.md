# Java-spring-shorturl —— 高并发短链接生成系统

将长 URL 转换为短链（如 `https://s.cn/aB3x9`），访问短链时 302 跳转回原地址。重点解决**高并发读写**场景下的发号、缓存、限流与统计问题。

> 技术栈：Spring Boot 2.7.18 · MyBatis-Plus 3.5.3 · MySQL 8 · Redis · Redisson · Caffeine · Guava BloomFilter · JJWT · ShedLock · JDK 17

## 架构总览

```
                       ┌──────────────────────────────────────────┐
   创建短链 POST /api/url                                         │
   ────────────────►  │  @Limiter AOP 限流（Lua 滑动窗口）         │
                      │        │                                  │
                      │        ▼                                  │
                      │  同 URL 复用检查（Redisson 分布式锁）      │
                      │        │                                  │
                      │        ▼                                  │
                      │  号段发号器（双 Buffer）──► id ──► Base62 ──► 短码
                      └──────────────────────────────────────────┘

   访问短链 GET /{code}
   ────────────────►  L1 Caffeine ──命中──► 302
                          │ 未命中
                          ▼
                      L2 Redis ─────命中──► 回填 L1 ──► 302
                          │ 未命中
                          ▼
                      布隆过滤器 ──不存在──► 直接 404（防穿透）
                          │ 可能存在
                          ▼
                      Redisson 互斥锁 ──► MySQL ──► 回填 L2/L1 ──► 302

   访问统计（旁路）
   ────────────────►  PV: Redis Hash 蓄水 ──► ShedLock 定时批量刷库
                      UV: HyperLogLog 估算
```

## 核心设计

### 1. 号段 + 双 Buffer 发号器
- 数据库行锁保证多实例取号段互不重叠，单次批量取号摊薄 DB 写压力
- 当前号段消耗到 **10% 水位**时异步预加载下一个号段，发号线程无感知切换
- 短码 = `Base62(id)`，纯进制转换、零哈希冲突

### 2. 多级读路径（防穿透 / 击穿 / 雪崩）
| 问题 | 方案 |
|---|---|
| 缓存穿透 | Guava 布隆过滤器前置拦截不存在的短码 |
| 缓存击穿 | Redisson 互斥锁 + 双重检查，只放一个线程回源 |
| 缓存雪崩 | 启动预热（ApplicationRunner）+ 差异化 TTL |
| Redis 故障 | 降级直查 MySQL，保证可用性 |

### 3. 滑动窗口限流（@Limiter）
- Lua 脚本在 Redis 端原子执行，无固定窗口的边界突刺问题
- 自研 `@Limiter` 注解 + AOP 切面，默认按 IP 限流，登录态自动切换 userId 维度

### 4. 访问统计旁路
- PV 不直接 UPDATE 数据库（防单行热点）：Redis Hash 蓄水，定时任务批量刷回
- UV 用 HyperLogLog（0.81% 标准误差）计数
- **ShedLock** 保证多实例部署时定时任务只有一个节点执行

### 5. 用户与鉴权
- 注册 / 登录，JWT 无状态鉴权，拦截器 + ThreadLocal 传递当前用户
- 全局 `ResponseBodyAdvice` 统一返回包装，`@NoWrap` 注解支持局部跳过（如 302 跳转接口）

## 项目结构

```
src/main/java/com/wcy/shorturl/
├── codec/          Base62 编解码
├── id/             号段发号器（Segment / SegmentBuffer / SegmentAllocator）
├── config/         Redisson、ShedLock、缓存、拦截器等配置
├── Cache/          启动预热（CachePreheatRunner）
├── Limt/           @Limiter 注解 + AOP 切面 + Lua 滑动窗口限流器
├── Interceptor/    JWT 登录拦截器
├── controller/     短链管理 / 302 跳转 / 统计查询
├── service/        业务逻辑（创建、重定向、统计刷库）
├── domain/         实体（po / pro / vo）
├── util/           JwtUtil、CurrentHolder、@NoWrap
└── common/         全局异常处理、统一返回
```

## 快速开始

```bash
# 1. 建库建表（MySQL 8）
mysql -u root -p < sql/init.sql

# 2. 修改配置
# src/main/resources/application.yml 中的 MySQL / Redis 连接信息

# 3. 启动
mvn spring-boot:run

# 4. 测试
mvn test
```

## API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/url/create` | 创建短链（同 URL 复用已有短码） |
| GET | `/{code}` | 302 跳转原始长链 |
| GET | `/api/stats/{code}` | 查询短链 PV / UV 统计 |
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 登录签发 JWT |

## 压测数据

> 待补充（JMeter）：计划记录单机 QPS、P99 延迟、缓存命中率、限流触发情况。

## License

Apache License 2.0
