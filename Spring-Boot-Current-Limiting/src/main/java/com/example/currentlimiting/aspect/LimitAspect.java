package com.example.currentlimiting.aspect;

import com.example.currentlimiting.annotation.Limit;
import com.example.currentlimiting.domain.LimitRange;
import com.example.currentlimiting.domain.LimitType;
import com.example.currentlimiting.exception.LimitAccessException;
import com.example.currentlimiting.utils.IPUtils;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author dengzhiming
 * @date 2020/2/28 23:30
 */
@Aspect
@Component
public class LimitAspect {
    private static final Logger logger = LoggerFactory.getLogger(LimitAspect.class);
    @Resource
    RedisTemplate<String, Serializable> limitRedisTemplate;

    @Pointcut("@annotation(com.example.currentlimiting.annotation.Limit)")
    public void pointcut() {
    }

    /**
     * @param point 切点
     * @return
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 从请求上下文中,通过请求属性获取请求对象
        HttpServletRequest request = ((ServletRequestAttributes) (Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))).getRequest();
        // 从切点对象中获取签名,并强转成方法签名对象
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 从方法签名对象获取方法对象
        Method method = signature.getMethod();
        // 从方法对象获取方法上的Limit注解对象
        Limit limitAnnotation = method.getAnnotation(Limit.class);
        // 获取方法上Limit注解中用于描述接口功能的属性值
        String name = limitAnnotation.name();
        // 获取方法上Limit注解中限流策略的时间长度值
        int limitPeriod = limitAnnotation.period();
        // 获取方法上Limit注解中限流策略的限制访问次数值
        int limitCount = limitAnnotation.count();
        // 获取方法上Limit注解的限流类型属性
        LimitType limitType = limitAnnotation.limitType();
        // 根据Limit注解的限流类型
        String key;
        switch (limitType) {
            case IP:
                key = IPUtils.getIpAddr(request);
                break;
            case GENERAL:
                key = limitAnnotation.key();
                break;
            default:
                key = StringUtils.upperCase(method.getName());
        }
        // 获取方法上Limit注解的限流范围属性
        LimitRange limitRange = limitAnnotation.limitRange();
        // 构建一个不可变集合
        ImmutableList<String> keys;
        if (limitRange == LimitRange.GLOBAL) {
            keys = ImmutableList.of(StringUtils.join(key + "_GLOBAL"));
        } else {
            //由于测试时,没有登录,所以sessionId一直为null
            keys = ImmutableList.of(StringUtils.join(key + "_" + request.getRequestedSessionId()));
        }
        // 构建一个限流的lua脚本
        String luaScript = buildLuaScript();
        // 使用lua脚本和脚本返回值类型,创建一个在redis执行的lua脚本对象
        RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
        // 执行lua脚本
        Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
        logger.info("第{}次访问key为 {}，描述为 [{}] 的接口", count, keys, name);
        // 如果访问次数不为null,且访问次数小于限制值则请求放行,否则抛出异常
        if (count != null && count.intValue() <= limitCount) {
            return point.proceed();
        }else {
            throw new LimitAccessException("接口访问超出频率限制");
        }
    }

    /**
     * 限流脚本
     *  1.脚本关键点解析:
     *      [1]redis.call('incr',KEYS[1]): 以集合中第一个key为键,对redis中存储的值加一,并返回
     *      [2]tonumber(c): 将变量c转成数值; tonumber(ARGV[1]): 将第一个参数转成数值;
     *      [3]redis.call('expire',KEYS[1],ARGV[2]): 以集合中第一个key为键,第一个参数为值,设置key的过期时间
     *  2.脚本意义:
     *      [1]在redis中对key进行加一操作(每访问一次加一),并返回加一后的值;(若key不存在,则其值会先被初始化为0,再执行INCR操作)
     *      [2]如果该key对应的值加1后等于1(即初次访问),则对该key设置过期时间
     *      [3]如果该key对应的值(当前用户访问的次数)大于设置的限制值,则返回该值
     * @return lua脚本
     */
    private String buildLuaScript() {
        return "local count" +
                "\n count = redis.call('incr',KEYS[1])" +
                "\n if tonumber(count) == 1 then" +
                "\n redis.call('expire',KEYS[1],ARGV[2])" +
                "\n end" +
                "\n if count and tonumber(count) > tonumber(ARGV[1]) then" +
                "\n return count;" +
                "\n end" +
                "\n return count;";
    }
}
