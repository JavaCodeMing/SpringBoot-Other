package com.example.sqlinterceptor.interceptor;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * @author dengzhiming
 * @date 2020/3/1 16:12
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlStatementInterceptor implements Interceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object returnValue;
        long start = System.currentTimeMillis();
        // 执行 SQL语句
        returnValue = invocation.proceed();
        long end = System.currentTimeMillis();
        // 耗时
        long time = end - start;
        try {
            // 获取SQL的传参
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = null;
            //获取参数,if语句成立,表示sql语句有参数,参数格式是map形式
            if (args.length > 1) {
                parameter = invocation.getArgs()[1];
            }
            // 获取到节点的 id,即 sql语句的 id
            String sqlId = ms.getId();
            // BoundSql就是封装 MyBatis最终产生的 sql类
            BoundSql boundSql = ms.getBoundSql(parameter);
            // 获取节点的配置
            Configuration configuration = ms.getConfiguration();
            // 获取到最终的 sql语句
            printSql(configuration, boundSql, sqlId, time);
        } catch (Exception e) {
            logger.error("sql拦截异常:{} ", e.getMessage());
        }
        return returnValue;
    }

    private void printSql(Configuration configuration, BoundSql boundSql, String sqlId, long time) {
        // 获取最终执行的完整SQL
        String sql = showSql(configuration, boundSql);
        logger.info("【SQL语句Id】>>>> {}", sqlId);
        logger.info("【SQL语句耗时】>>>> {}ms", time);
        logger.info("【SQL语句】>>>> {}", sql);
    }

    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        // sql语句中多个空格都用一个空格代替
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (!CollectionUtils.isEmpty(parameterMappings) && parameterObject != null) {
            // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换　　　　　　
            // 判断是否类型处理器注册器中是否含有参数对象类型
            TypeHandlerRegistry handlerRegistry = configuration.getTypeHandlerRegistry();
            if (handlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                // 把SQL中"?"用参数替换,且对参数的格式进行调整
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                // MetaObject主要是封装了 originalObject对象,提供了 get和 set的方法用于获取和设置 originalObject的属性值
                // 主要支持对 JavaBean、Collection、Map三种类型对象的操作
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    // 从参数映射对象中获取属性名
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object value = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(value)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 该分支是动态 sql
                        Object value = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(value)));
                    } else {
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql;
    }

    private String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DATE_FIELD, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            }else {
                value = "";
            }
        }
        return value;
    }
}
