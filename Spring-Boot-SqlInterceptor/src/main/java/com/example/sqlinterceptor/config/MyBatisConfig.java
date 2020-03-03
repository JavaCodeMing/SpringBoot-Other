package com.example.sqlinterceptor.config;

import com.example.sqlinterceptor.interceptor.SqlStatementInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * sql打印拦截器配置
 * @author dengzhiming
 * @date 2020/3/1 16:11
 */
@Configuration
public class MyBatisConfig {

    // 当yml文件中showsql属性的值为true时,该配置生效
    @Bean
    @ConditionalOnProperty(name = "showsql", havingValue = "true")
    SqlStatementInterceptor sqlStatementInterceptor(){
        return new SqlStatementInterceptor();
    }
}
