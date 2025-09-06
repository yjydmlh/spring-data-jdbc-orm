package io.flexdata.spring.orm.core.table;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 表名切换切面
 * 处理@Table注解，实现动态表名切换
 */
@Aspect
@Component
@Order(1) // 确保在数据源切换之后执行
public class TableAspect {
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * 拦截带有@Table注解的方法
     */
    @Around("@annotation(table)")
    public Object handleTableSwitch(ProceedingJoinPoint joinPoint, Table table) throws Throwable {
        // 解析表名映射
        Map<String, String> tableMappings = parseTableMappings(table, joinPoint);
        
        if (tableMappings.isEmpty()) {
            // 没有表名映射，直接执行
            return joinPoint.proceed();
        }
        
        // 在表名映射作用域内执行
        return TableContext.executeWithTableMappings(tableMappings, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * 拦截类级别的@Table注解
     */
    @Around("@within(table) && !@annotation(io.flexdata.spring.orm.core.table.Table)")
    public Object handleClassLevelTableSwitch(ProceedingJoinPoint joinPoint, Table table) throws Throwable {
        // 解析类级别的表名映射
        Map<String, String> tableMappings = parseTableMappings(table, joinPoint);
        
        if (tableMappings.isEmpty()) {
            return joinPoint.proceed();
        }
        
        // 在表名映射作用域内执行
        return TableContext.executeWithTableMappings(tableMappings, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * 解析表名映射配置
     */
    private Map<String, String> parseTableMappings(Table table, ProceedingJoinPoint joinPoint) {
        Map<String, String> mappings = new HashMap<>();
        
        // 处理value数组配置
        if (table.value().length > 0) {
            for (String mapping : table.value()) {
                if (StringUtils.hasText(mapping)) {
                    String resolvedMapping = resolveExpression(mapping, joinPoint);
                    parseMapping(resolvedMapping, mappings);
                }
            }
        }
        
        // 处理单个表名映射
        if (StringUtils.hasText(table.logicalName()) && StringUtils.hasText(table.physicalName())) {
            String logicalName = resolveExpression(table.logicalName(), joinPoint);
            String physicalName = resolveExpression(table.physicalName(), joinPoint);
            mappings.put(logicalName, physicalName);
        }
        
        return mappings;
    }
    
    /**
     * 解析单个映射字符串
     * 格式："logicalTableName:physicalTableName"
     */
    private void parseMapping(String mapping, Map<String, String> mappings) {
        if (!StringUtils.hasText(mapping)) {
            return;
        }
        
        String[] parts = mapping.split(":");
        if (parts.length == 2) {
            String logicalName = parts[0].trim();
            String physicalName = parts[1].trim();
            if (StringUtils.hasText(logicalName) && StringUtils.hasText(physicalName)) {
                mappings.put(logicalName, physicalName);
            }
        }
    }
    
    /**
     * 解析SpEL表达式
     */
    private String resolveExpression(String expressionString, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(expressionString)) {
            return expressionString;
        }
        
        // 如果不包含SpEL表达式标记，直接返回
        if (!expressionString.contains("#{") && !expressionString.contains("${")) {
            return expressionString;
        }
        
        try {
            // 创建评估上下文
            EvaluationContext context = createEvaluationContext(joinPoint);
            
            // 解析并评估表达式
            Expression expression = expressionParser.parseExpression(expressionString);
            Object result = expression.getValue(context);
            
            return result != null ? result.toString() : expressionString;
        } catch (Exception e) {
            // 表达式解析失败，返回原始字符串
            return expressionString;
        }
    }
    
    /**
     * 创建SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 设置方法参数
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
                context.setVariable("p" + i, args[i]); // 简短形式
            }
            context.setVariable("args", args);
        }
        
        // 设置目标对象
        context.setVariable("target", joinPoint.getTarget());
        
        // 设置方法信息
        if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
            org.aspectj.lang.reflect.MethodSignature methodSignature = 
                (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            context.setVariable("method", method);
            context.setVariable("methodName", method.getName());
            
            // 设置参数名称映射
            String[] parameterNames = methodSignature.getParameterNames();
            if (parameterNames != null && args != null) {
                for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
        }
        
        return context;
    }
}