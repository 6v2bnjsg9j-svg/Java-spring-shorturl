package com.wcy.shorturl.id;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;


public class Segment {
    private final long start;
    private final long max;
    private final AtomicLong current;
    public Segment(long start, long max) {
        this.start = start;
        this.max = max;
        this.current = new AtomicLong(start - 1);
    }
    public long next(){
        long v = current.incrementAndGet();
        return v < max ? v : -1;
    }
    public double getUsed()
    {
        long total = current.get() - start+1;
        if(total <=0)
            return 1.0;
        return (double)(current.get()-start)/total;
    }
    public long getStart()
    {
        return start;
    }
    public long getMax()
    {
        return max;
    }

}
