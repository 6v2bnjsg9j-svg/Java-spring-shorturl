package com.wcy.shorturl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
public class AsyncConfig {

    @Bean("statsExecutor")
    public Executor statsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        // 4. 核心线程数。即使线程池空闲，也会保留 2 个线程存活（除非开启 allowCoreThreadTimeOut）。

        executor.setMaxPoolSize(4);
        // 5. 最大线程数。当核心线程忙不过来，且队列也满了时，最多扩容到 4 个线程。

        executor.setQueueCapacity(10000);
        // 6. 任务队列容量。当核心线程都在忙时，新来的任务会放到这个容量为 10000 的阻塞队列中排队。

        executor.setThreadNamePrefix("stats-");
        // 7. 线程名前缀。极其重要！线上排查日志或 jstack 时，能看到 "stats-1", "stats-2"，方便定位问题。
        executor.setKeepAliveSeconds(60);
        //非核心线程最多存活60s
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 8. 拒绝策略。当线程数达到 maxPoolSize(4) 且队列(10000)也塞满时，提交任务的线程（比如 Tomcat 线程）会亲自执行该任务。
        // 这是一种“背压”机制，保证任务不丢失，但会阻塞主线程。

        executor.initialize();
        // 9. 手动初始化。因为 ThreadPoolTaskExecutor 实现了 InitializingBean，Spring 容器启动时会自动调用它。
        // 这里显式调用也没错（双重保险），会创建底层的线程池。

        return executor;
        // 10. 将配置好的线程池实例返回给 Spring 容器。
    }
//    .生命周期与优雅停机（生产环境强烈建议配置）
//    setKeepAliveSeconds(int keepAliveSeconds)：非核心线程的空闲存活时间（默认 60 秒）。超过这个时间，非核心线程会被回收。
//
//    setAllowCoreThreadTimeOut(boolean)：是否允许核心线程超时回收（默认 false）。如果设为 true，核心线程空闲后也会被销毁。
//
//    setWaitForTasksToCompleteOnShutdown(boolean)：优雅停机。当 Spring 容器关闭时，是否等待队列中的任务全部执行完毕再退出（默认 false，直接中断）。
//
//    setAwaitTerminationSeconds(int)：优雅停机的超时时间。配合上面那个属性使用，比如等待 60 秒还没跑完，就强制退出。
//
//            2. 上下文传递与装饰（排查链路问题必备）
//    setTaskDecorator(TaskDecorator taskDecorator)：极度推荐！ 用于装饰任务。比如在 Web 应用中，主线程的 ThreadLocal（如 MDC 日志 TraceId、用户上下文）是无法直接传递给子线程的。通过 TaskDecorator 可以拷贝上下文给异步线程。
//
//            3. 底层定制
//    setThreadFactory(ThreadFactory threadFactory)：自定义线程工厂。如果你觉得 setThreadNamePrefix 不够用，可以自己实现 ThreadFactory，比如给线程设置优先级、设置为守护线程等。
//
//    setTaskExecutor(Executor executor)：直接注入一个原生 Java 的 ThreadPoolExecutor，Spring 会在这个底层执行器之上包装。
//
//    setBeanName(String name)：如果通过编程式定义 Bean，可以设置名字。
//
//            4. 拒绝策略（四种标准策略）
//    你目前用的是 CallerRunsPolicy，其他三种可以了解下：
//
//    AbortPolicy（默认）：直接抛出 RejectedExecutionException，丢弃任务。
//
//    DiscardPolicy：默默丢弃，什么都不做，也不报错（非常危险）。
//
//    DiscardOldestPolicy：丢弃队列中最老的任务，然后尝试提交新任务。
//
//    CallerRunsPolicy（你选的）：谁提交，谁执行。
}

