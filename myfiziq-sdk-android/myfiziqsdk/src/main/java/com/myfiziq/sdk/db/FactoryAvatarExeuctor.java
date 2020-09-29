package com.myfiziq.sdk.db;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolExecutor provides us a list of PENDING runnables to be exeucted through the {@link ThreadPoolExecutor.CallerRunsPolicy#getQueue()} method.
 *
 * This class allows us to track all pending runnables and those that are currently executing.
 */
public class FactoryAvatarExeuctor extends ThreadPoolExecutor
{
    private ConcurrentLinkedQueue<Runnable> runnableList = new ConcurrentLinkedQueue<>();

    public FactoryAvatarExeuctor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public FactoryAvatarExeuctor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public FactoryAvatarExeuctor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public FactoryAvatarExeuctor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler)
    {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)
    {
        super.beforeExecute(thread, runnable);

        if (!runnableList.contains(runnable))
        {
            runnableList.add(runnable);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t)
    {
        super.afterExecute(r, t);

        runnableList.remove(r);
    }


    public ConcurrentLinkedQueue<Runnable> getPendingRunnables()
    {
        return runnableList;
    }
}
