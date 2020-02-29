package com.example.currentlimiting.annotation;

import com.example.currentlimiting.domain.LimitRange;
import com.example.currentlimiting.domain.LimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author dengzhiming
 * @date 2020/2/28 23:20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Limit {
    //资源名称,用于描述接口功能
    String name()default "";
    //资源的key
    String key() default "";
    //时间跨度,单位秒
    int period();
    //在时间跨度内限制的访问次数
    int count();
    // 限制类型(默认传统类型)
    LimitType limitType() default LimitType.GENERAL;
    // 限制范围(默认针对用户)
    LimitRange limitRange() default LimitRange.CONSUMER;
}
