package com.spring.jdbc.orm.aspect;

import com.spring.jdbc.orm.annotation.DataSource;
import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 数据源切换AOP切面
 * 处理@DataSource注解，实现自动数据源切换
 */
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class DataSourceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);
    
    /**
     * 切点：所有标注了@DataSource注解的方法
     */
    @Pointcut("@annotation(com.spring.jdbc.orm.annotation.DataSource)")
    public void dataSourcePointcut() {}
    
    /**
     * 切点：类级别的@DataSource注解
     */
    @Pointcut("@within(com.spring.jdbc.orm.annotation.DataSource)")
    public void dataSourceClassPointcut() {}
    
    /**
     * 环绕通知：处理数据源切换
     */
    @Around("dataSourcePointcut() || dataSourceClassPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataSource dataSource = getDataSource(point);
        
        if (dataSource == null) {
            return point.proceed();
        }
        
        String dataSourceKey = determineDataSourceKey(dataSource);
        String originalDataSource = DataSourceContext.getDataSource();
        
        try {
            if (StringUtils.hasText(dataSourceKey)) {
                DataSourceContext.setDataSource(dataSourceKey);
                if (logger.isDebugEnabled()) {
                    logger.debug("Switch to datasource: {} for method: {}", 
                               dataSourceKey, point.getSignature().toShortString());
                }
            }
            
            return point.proceed();
            
        } finally {
            // 恢复原始数据源设置
            if (originalDataSource != null) {
                DataSourceContext.setDataSource(originalDataSource);
            } else {
                DataSourceContext.clearDataSource();
            }
            
            if (logger.isDebugEnabled() && StringUtils.hasText(dataSourceKey)) {
                logger.debug("Restore datasource from: {} for method: {}", 
                           dataSourceKey, point.getSignature().toShortString());
            }
        }
    }
    
    /**
     * 获取方法或类上的@DataSource注解
     */
    private DataSource getDataSource(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        // 优先获取方法级别的注解
        DataSource dataSource = AnnotationUtils.findAnnotation(method, DataSource.class);
        
        // 如果方法上没有，则获取类级别的注解
        if (dataSource == null) {
            dataSource = AnnotationUtils.findAnnotation(method.getDeclaringClass(), DataSource.class);
        }
        
        return dataSource;
    }
    
    /**
     * 确定数据源key
     */
    private String determineDataSourceKey(DataSource dataSource) {
        // 优先使用value属性
        if (StringUtils.hasText(dataSource.value()) && !"default".equals(dataSource.value())) {
            return dataSource.value();
        }
        
        // 使用type属性
        return dataSource.type().getKey();
    }
}