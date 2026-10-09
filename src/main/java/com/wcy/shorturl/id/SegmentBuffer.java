package com.wcy.shorturl.id;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SegmentBuffer {

    private final Segment[] slots = new Segment[2];
    private String bizTag;

    private volatile int idex=0;
    private volatile Boolean isReady=false;
    //阈值
    private static final double Used=0.1;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    public SegmentBuffer(String bizTag) {
        this.bizTag = bizTag;
    }



    public synchronized long takeNext(Loader loader, ExecutorService executors) {
        while(true)
        {
            if(slots[idex]==null)
            {
                //初始化
                slots[idex] = loader.load();
                log.info("初始化号段");
                continue;
            }
            Segment slot = slots[idex];
            long next = slot.next();
            if(next!=-1)
            {
                //预加载下一段的号码
                if(isReady==false
                        &&slot.getUsed()>=Used
                        &&loading.compareAndSet(false,true))
                {
                   executors.execute(()->{
                       try{
                           addSegment(loader.load());
                           log.info("预加载下一次的号完成");
                       }
                       catch(Exception e)
                       {
                           loading.set(false);
                           log.warn("[{}] 异步预加载号段失败: {}", bizTag, e.getMessage());
                       }
                   });
                }
                return next;
            }
            //------号码发完转换----
            if(isReady)
            {
                switchSlot();
                continue;
            }
            log.warn("[{}] 双Buffer未命中，退化为同步加载", bizTag);
            addSegment(loader.load());
            switchSlot();
        }
    }

    private void switchSlot(){
        idex^=1;
        isReady=false;
    }
    private synchronized void addSegment(Segment segment){
        slots[idex^1]=segment;
        isReady=true;
        loading.set(false);
        log.info("[{}] 预加载号段就绪: [{}-{}]", bizTag, segment.getStart(), segment.getMax());
    }
    @FunctionalInterface
    public interface Loader
    {
        Segment load();
    }
}
