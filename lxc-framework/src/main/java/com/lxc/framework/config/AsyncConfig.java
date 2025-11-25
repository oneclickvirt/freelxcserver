package com.lxc.framework.config;

import cn.hutool.core.util.ArrayUtil;
import com.lxc.common.exception.ServiceException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置
 *
 * @author Lion Li
 */
@EnableAsync
@Configuration
public class AsyncConfig extends AsyncConfigurerSupport {
    @Bean(name = "lxcAsyncExecutor") // Bean 名称必须和 @Async 指定的名称完全匹配
    public ThreadPoolTaskExecutor lxcAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1. 核心线程数：线程池长期维持的最小线程数（即使空闲也不回收）
        // 建议值：CPU 核心数 * 2（比如 4核CPU 设为 8），根据任务并发量调整
        executor.setCorePoolSize(8);
        // 2. 最大线程数：线程池允许的最大线程数（并发峰值）
        // 建议值：核心线程数 + 备用线程数（避免线程过多导致上下文切换消耗）
        executor.setMaxPoolSize(16);
        // 3. 任务队列容量：核心线程数忙时，任务先存入队列，队列满了才创建新线程（直到最大线程数）
        // 建议值：20-100（根据任务提交频率调整，太大容易积压任务占用内存）
        executor.setQueueCapacity(50);
        // 4. 空闲线程存活时间：超过核心线程数的"临时线程"，空闲多久后回收（单位：秒）
        // 建议值：30-60秒（避免频繁创建/销毁临时线程）
        executor.setKeepAliveSeconds(60);
        // 5. 线程名前缀：日志/监控时能快速识别该线程属于哪个任务池（便于排查问题）
        executor.setThreadNamePrefix("Lxc-Async-");
        // 6. 拒绝策略：当 线程数达最大 + 队列满 时，新任务如何处理（核心！避免任务丢失）
        // 推荐：CallerRunsPolicy（让提交任务的调用线程自己执行，避免任务丢失，同时自然限流）
        // 其他可选策略：
        // - AbortPolicy（默认，直接抛异常，不推荐）
        // - DiscardPolicy（悄悄丢弃任务，不推荐）
        // - DiscardOldestPolicy（丢弃队列最老的任务，可能丢关键任务，不推荐）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 7. 初始化线程池（必须调用，否则线程池不会生效）
        executor.initialize();
        return executor;
    }

    @Autowired
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 自定义 @Async 注解使用系统线程池
     */
    @Override
    public Executor getAsyncExecutor() {
        return scheduledExecutorService;
    }

    /**
     * 异步执行异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            throwable.printStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("Exception message - ").append(throwable.getMessage())
                .append(", Method name - ").append(method.getName());
            if (ArrayUtil.isNotEmpty(objects)) {
                sb.append(", Parameter value - ").append(Arrays.toString(objects));
            }
            throw new ServiceException(sb.toString());
        };
    }

}
