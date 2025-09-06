package io.flexdata.spring.orm.routing.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 路由上下文
 * 包含路由决策所需的所有上下文信息
 */
public class RoutingContext {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 方法参数
     */
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * HTTP请求头
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * 用户信息
     */
    private Object userInfo;

    /**
     * 额外属性
     */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        SELECT, INSERT, UPDATE, DELETE, BATCH_INSERT, BATCH_UPDATE, BATCH_DELETE
    }

    public RoutingContext() {
    }

    public RoutingContext(String tableName, OperationType operationType) {
        this.tableName = tableName;
        this.operationType = operationType;
    }

    /**
     * 判断是否为读操作
     */
    public boolean isReadOperation() {
        return operationType == OperationType.SELECT;
    }

    /**
     * 判断是否为写操作
     */
    public boolean isWriteOperation() {
        return !isReadOperation();
    }

    /**
     * 判断是否为批量操作
     */
    public boolean isBatchOperation() {
        return operationType == OperationType.BATCH_INSERT ||
               operationType == OperationType.BATCH_UPDATE ||
               operationType == OperationType.BATCH_DELETE;
    }

    /**
     * 添加参数
     */
    public RoutingContext addParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * 添加多个参数
     */
    public RoutingContext addParameters(Map<String, Object> params) {
        if (params != null) {
            this.parameters.putAll(params);
        }
        return this;
    }

    /**
     * 获取参数
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * 获取参数（带默认值）
     */
    public Object getParameter(String key, Object defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串参数
     */
    public String getStringParameter(String key) {
        Object value = getParameter(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取整数参数
     */
    public Integer getIntParameter(String key) {
        Object value = getParameter(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取长整数参数
     */
    public Long getLongParameter(String key) {
        Object value = getParameter(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 添加请求头
     */
    public RoutingContext addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * 添加多个请求头
     */
    public RoutingContext addHeaders(Map<String, String> headers) {
        if (headers != null) {
            this.headers.putAll(headers);
        }
        return this;
    }

    /**
     * 获取请求头
     */
    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * 获取请求头（带默认值）
     */
    public String getHeader(String key, String defaultValue) {
        return headers.getOrDefault(key, defaultValue);
    }

    /**
     * 添加属性
     */
    public RoutingContext addAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    /**
     * 获取属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取属性（带默认值）
     */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private RoutingContext context = new RoutingContext();

        public Builder tableName(String tableName) {
            context.tableName = tableName;
            return this;
        }

        public Builder operationType(OperationType operationType) {
            context.operationType = operationType;
            return this;
        }

        public Builder parameter(String key, Object value) {
            context.addParameter(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            context.addParameters(parameters);
            return this;
        }

        public Builder header(String key, String value) {
            context.addHeader(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            context.addHeaders(headers);
            return this;
        }

        public Builder userInfo(Object userInfo) {
            context.userInfo = userInfo;
            return this;
        }

        public Builder attribute(String key, Object value) {
            context.addAttribute(key, value);
            return this;
        }

        public RoutingContext build() {
            return context;
        }
    }

    // Getters and Setters
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(Object userInfo) {
        this.userInfo = userInfo;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "RoutingContext{" +
                "tableName='" + tableName + '\'' +
                ", operationType=" + operationType +
                ", parameters=" + parameters +
                ", headers=" + headers +
                ", userInfo=" + userInfo +
                ", attributes=" + attributes +
                '}';
    }
}