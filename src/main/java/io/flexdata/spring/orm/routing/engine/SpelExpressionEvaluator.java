package io.flexdata.spring.orm.routing.engine;

import io.flexdata.spring.orm.routing.context.RoutingContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * SpEL表达式评估器
 * 支持在路由规则中使用Spring表达式语言
 */
@Component
public class SpelExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 表达式缓存
     */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 评估条件表达式
     * 
     * @param expression SpEL表达式
     * @param context 路由上下文
     * @return 评估结果
     */
    public boolean evaluateCondition(String expression, RoutingContext context) {
        if (!StringUtils.hasText(expression)) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }
        
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        try {
            Expression expr = getExpression(expression);
            StandardEvaluationContext evalContext = createEvaluationContext(context);
            Object result = expr.getValue(evalContext);
            
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else {
                // 非布尔结果抛出ClassCastException
                throw new ClassCastException("Condition expression must return boolean, but got: " + 
                    (result != null ? result.getClass().getSimpleName() : "null"));
            }
        } catch (ClassCastException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Error evaluating SpEL condition: " + expression, e);
        }
    }

    /**
     * 评估表达式并返回指定类型的结果
     * 
     * @param expression SpEL表达式
     * @param context 路由上下文
     * @param resultType 结果类型
     * @param <T> 结果类型
     * @return 评估结果
     */
    public <T> T evaluateExpression(String expression, RoutingContext context, Class<T> resultType) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        try {
            Expression expr = getExpression(expression);
            StandardEvaluationContext evalContext = createEvaluationContext(context);
            return expr.getValue(evalContext, resultType);
        } catch (Exception e) {
            throw new EvaluationException("Error evaluating SpEL expression: " + expression, e);
        }
    }

    /**
     * 评估表达式并返回Object结果
     * 
     * @param expression SpEL表达式
     * @param context 路由上下文
     * @return 评估结果
     */
    public Object evaluateExpression(String expression, RoutingContext context) {
        return evaluateExpression(expression, context, Object.class);
    }

    /**
     * 获取表达式（带缓存）
     */
    private Expression getExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, expr -> {
            // 如果表达式被#{...}包围，则去掉包围符号
            String actualExpression = expr;
            if (expr.startsWith("#{") && expr.endsWith("}")) {
                actualExpression = expr.substring(2, expr.length() - 1);
            }
            return parser.parseExpression(actualExpression);
        });
    }

    /**
     * 创建评估上下文
     */
    private StandardEvaluationContext createEvaluationContext(RoutingContext context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        
        if (context != null) {
            // 设置根对象为路由上下文
            evalContext.setRootObject(context);
            
            // 注册常用变量
            evalContext.setVariable("context", context);
            evalContext.setVariable("tableName", context.getTableName());
            evalContext.setVariable("operationType", context.getOperationType());
            evalContext.setVariable("parameters", context.getParameters());
            evalContext.setVariable("headers", context.getHeaders());
            evalContext.setVariable("userInfo", context.getUserInfo());
            evalContext.setVariable("attributes", context.getAttributes());
            
            // 注册便捷方法
            evalContext.setVariable("isRead", context.isReadOperation());
            evalContext.setVariable("isWrite", context.isWriteOperation());
            evalContext.setVariable("isBatch", context.isBatchOperation());
        }
        
        // 注册工具函数（总是注册）
        evalContext.setVariable("utils", new SpelUtils());
        
        // 注册随机数生成器（总是注册）
        evalContext.setVariable("random", new java.util.Random());
        
        // 注册时间变量（总是注册）
        evalContext.setVariable("now", java.time.LocalDateTime.now());
        
        return evalContext;
    }

    /**
     * 清空表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return expressionCache.size();
    }

    /**
     * SpEL工具类
     * 提供在表达式中使用的工具方法
     */
    public static class SpelUtils {
        
        /**
         * 检查字符串是否为空
         */
        public static boolean isEmpty(String str) {
            return !StringUtils.hasText(str);
        }
        
        /**
         * 检查字符串是否不为空
         */
        public static boolean isNotEmpty(String str) {
            return StringUtils.hasText(str);
        }
        
        /**
         * 字符串包含检查
         */
        public static boolean contains(String str, String substring) {
            return str != null && str.contains(substring);
        }
        
        /**
         * 字符串前缀检查
         */
        public static boolean startsWith(String str, String prefix) {
            return str != null && str.startsWith(prefix);
        }
        
        /**
         * 字符串后缀检查
         */
        public static boolean endsWith(String str, String suffix) {
            return str != null && str.endsWith(suffix);
        }
        
        /**
         * 正则表达式匹配
         */
        public static boolean matches(String str, String regex) {
            return str != null && str.matches(regex);
        }
        
        /**
         * 获取当前时间戳
         */
        public static long currentTimeMillis() {
            return System.currentTimeMillis();
        }
        
        /**
         * 获取当前日期（YYYYMMDD格式）
         */
        public static String currentDate() {
            return String.format("%tY%tm%td", System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
        }
        
        /**
         * 获取当前小时
         */
        public static int currentHour() {
            return java.time.LocalTime.now().getHour();
        }
        
        /**
         * 哈希取模
         */
        public static int hashMod(Object value, int mod) {
            if (value == null) {
                return 0;
            }
            return Math.abs(value.hashCode()) % mod;
        }
        
        /**
         * 字符串哈希取模
         */
        public static int stringHashMod(String str, int mod) {
            if (str == null) {
                return 0;
            }
            return Math.abs(str.hashCode()) % mod;
        }
        
        /**
         * 数值范围检查
         */
        public static boolean inRange(Number value, Number min, Number max) {
            if (value == null) {
                return false;
            }
            double val = value.doubleValue();
            double minVal = min != null ? min.doubleValue() : Double.MIN_VALUE;
            double maxVal = max != null ? max.doubleValue() : Double.MAX_VALUE;
            return val >= minVal && val <= maxVal;
        }
        
        /**
         * 字符串转整数
         */
        public static Integer toInt(String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        /**
         * 字符串转长整数
         */
        public static Long toLong(String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        /**
         * 安全的字符串比较
         */
        public static boolean equals(Object obj1, Object obj2) {
            if (obj1 == null && obj2 == null) {
                return true;
            }
            if (obj1 == null || obj2 == null) {
                return false;
            }
            return obj1.toString().equals(obj2.toString());
        }
        
        /**
         * 获取随机数
         */
        public static int random(int bound) {
            return (int) (Math.random() * bound);
        }
        
        /**
         * 检查集合是否包含元素
         */
        public static boolean containsAny(java.util.Collection<?> collection, Object... elements) {
            if (collection == null || elements == null) {
                return false;
            }
            for (Object element : elements) {
                if (collection.contains(element)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * 检查Map是否包含键
         */
        public static boolean hasKey(Map<?, ?> map, Object key) {
            return map != null && map.containsKey(key);
        }
        
        /**
         * 从Map中安全获取值
         */
        public static Object getValue(Map<?, ?> map, Object key) {
            return map != null ? map.get(key) : null;
        }
        
        /**
         * 从Map中获取字符串值
         */
        public static String getStringValue(Map<?, ?> map, Object key) {
            Object value = getValue(map, key);
            return value != null ? value.toString() : null;
        }
        
        /**
         * 哈希取模（测试需要的方法）
         */
        public static int hash(String value, int mod) {
            if (value == null) {
                return 0;
            }
            return Math.abs(value.hashCode()) % mod;
        }
        
        /**
         * 从Map中获取值，支持默认值（测试需要的方法）
         */
        public static Object getMapValue(Map<?, ?> map, Object key, Object defaultValue) {
            if (map == null) {
                return defaultValue;
            }
            Object value = map.get(key);
            return value != null ? value : defaultValue;
        }
        
        /**
         * 从List中获取值，支持默认值（测试需要的方法）
         */
        public static Object getListValue(java.util.List<?> list, int index, Object defaultValue) {
            if (list == null || index < 0 || index >= list.size()) {
                return defaultValue;
            }
            Object value = list.get(index);
            return value != null ? value : defaultValue;
        }
    }
}