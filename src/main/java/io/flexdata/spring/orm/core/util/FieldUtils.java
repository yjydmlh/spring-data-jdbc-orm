package io.flexdata.spring.orm.core.util;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段工具类，用于从Lambda表达式中提取字段名
 * 文件位置: src/main/java/com/example/orm/core/util/FieldUtils.java
 */
public class FieldUtils {
    private static final Map<String, String> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 通过Lambda表达式获取字段名
     * 例如: getFieldName(User::getUserName) -> "userName"
     */
    public static <T> String getFieldName(SFunction<T, ?> function) {
        String key = function.getClass().getName();
        return FIELD_NAME_CACHE.computeIfAbsent(key, k -> {
            try {
                // 通过序列化获取Lambda信息
                Method writeReplaceMethod = function.getClass().getDeclaredMethod("writeReplace");
                writeReplaceMethod.setAccessible(true);
                SerializedLambda serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(function);

                // 解析方法名获取字段名
                String methodName = serializedLambda.getImplMethodName();
                String fieldName;

                if (methodName.startsWith("get")) {
                    fieldName = methodName.substring(3);
                } else if (methodName.startsWith("is")) {
                    fieldName = methodName.substring(2);
                } else {
                    throw new IllegalArgumentException("Invalid getter method: " + methodName);
                }

                // 首字母小写
                if (fieldName.length() == 1) {
                    return fieldName.toLowerCase();
                } else {
                    return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get field name from lambda: " + function, e);
            }
        });
    }

    /**
     * 获取多个字段名
     */
    @SafeVarargs
    public static <T> String[] getFieldNames(SFunction<T, ?>... functions) {
        return Arrays.stream(functions)
                .map(FieldUtils::getFieldName)
                .toArray(String[]::new);
    }

    /**
     * 清空字段名缓存
     */
    public static void clearCache() {
        FIELD_NAME_CACHE.clear();
    }

    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return FIELD_NAME_CACHE.size();
    }
}
