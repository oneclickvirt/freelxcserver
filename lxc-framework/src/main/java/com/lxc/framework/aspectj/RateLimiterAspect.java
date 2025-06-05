package com.lxc.framework.aspectj;

import com.lxc.common.annotation.RateLimiter;
import com.lxc.common.enums.LimitType;
import com.lxc.common.exception.ServiceException;
import com.lxc.common.utils.MessageUtils;
import com.lxc.common.utils.ServletUtils;
import com.lxc.common.utils.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 限流处理切面类
 * 用于实现接口访问频率限制，防止系统被恶意请求攻击
 * 支持基于IP、集群和全局的限流策略
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    /**
     * 限流切面处理
     * 在目标方法执行前进行限流判断
     *
     * @param point 切点
     * @param rateLimiter 限流注解
     * @throws Throwable 限流异常
     */
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {
        // 获取限流时间窗口
        int time = rateLimiter.time();
        // 获取限流次数
        int count = rateLimiter.count();
        // 获取限流key
        String combineKey = getCombineKey(rateLimiter, point);
        try {
            // 默认使用全局限流
            RateType rateType = RateType.OVERALL;
            // 如果是集群限流，则使用每个客户端限流
            if (rateLimiter.limitType() == LimitType.CLUSTER) {
                rateType = RateType.PER_CLIENT;
            }
            // 执行限流操作，返回剩余令牌数
            long number = RedisUtils.rateLimiter(combineKey, rateType, count, time);
            // 如果返回-1，表示限流
            if (number == -1) {
                throw new ServiceException(MessageUtils.message("rate.limiter.message"));
            }
            log.info("限制令牌 => {}, 剩余令牌 => {}, 缓存key => '{}'", count, number, combineKey);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    /**
     * 获取限流key
     * 根据限流类型和切点信息生成唯一的限流key
     *
     * @param rateLimiter 限流注解
     * @param point 切点
     * @return 限流key
     */
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuilder stringBuffer = new StringBuilder(rateLimiter.key());
        if (rateLimiter.limitType() == LimitType.IP) {
            // 获取请求ip，用于IP限流
            stringBuffer.append(ServletUtils.getClientIP()).append("-");
        } else if (rateLimiter.limitType() == LimitType.CLUSTER) {
            // 获取客户端实例id，用于集群限流
            stringBuffer.append(RedisUtils.getClient().getId()).append("-");
        }
        // 获取请求调用的类名及方法名，用于生成唯一的限流key
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }
}
