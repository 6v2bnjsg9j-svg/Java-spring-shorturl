package com.wcy.shorturl.id;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wcy.shorturl.domain.po.TIdSegment;
import com.wcy.shorturl.mapper.TIdSegmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "short-url.id-strategy", havingValue = "segment", matchIfMissing = true)
public class SegmentAllocator implements IdGenerator , DisposableBean {
    @Autowired
    private  TIdSegmentMapper idSegmentMapper;
    private final Map<String, SegmentBuffer> buffers = new ConcurrentHashMap<>();


    private ExecutorService executor =  new ThreadPoolExecutor(
            1,
            1,
            30L,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(256),
            r->{
                Thread thread = new Thread(r, "SegmentAllocator");
                thread.setDaemon(true);
                return thread;
            });


//    public ThreadPoolExecutor(int corePoolSize,
//                              int maximumPoolSize,
//                              long keepAliveTime,
//                              TimeUnit unit,
//                              BlockingQueue<Runnable> workQueue) {
//        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
//                Executors.defaultThreadFactory(), defaultHandler);
//    }
    @Override
    public long nextId(String bizTag) {
        SegmentBuffer buffer=buffers.computeIfAbsent(bizTag, SegmentBuffer::new);
        return buffer.takeNext(()->{
            return getnew(bizTag);
        },executor);
    }
    private Segment getnew(String bizTag) {
        int i = idSegmentMapper.updateMaxid(bizTag);
        if(i==0)
        {
            throw new RuntimeException("发号器未准备好");
        }
        TIdSegment tIdSegment = idSegmentMapper.selectOne(new LambdaQueryWrapper<TIdSegment>().eq(TIdSegment::getBizTag, bizTag));
        Segment segment=new Segment(tIdSegment.getMaxId()-tIdSegment.getStep()+1,tIdSegment.getMaxId());
        return segment;
    }
    @Override
    public void destroy() throws Exception {
    executor.shutdown();
    }
}
