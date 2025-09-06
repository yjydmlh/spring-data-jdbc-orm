package io.flexdata.spring.orm.routing.selector;

import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于规则的数据源选择器
 * 支持多种规则匹配策略
 */
@Component
public class RuleBasedDataSourceSelector implements DataSourceSelector {

    @Autowired(required = false)
    private SpelExpressionEvaluator spelEvaluator;

    /**
     * 规则缓存
     */
    private final Map<String, List<SelectionRule>> ruleCache = new ConcurrentHashMap<>();

    /**
     * 选择规则
     */
    public static class SelectionRule {
        private String name;
        private String condition;
        private String dataSource;
        private int priority;
        private boolean enabled = true;
        private RuleType type = RuleType.SPEL;

        public SelectionRule() {}

        public SelectionRule(String name, String condition, String dataSource) {
            this.name = name;
            this.condition = condition;
            this.dataSource = dataSource;
        }

        public SelectionRule(String name, String condition, String dataSource, int priority) {
            this(name, condition, dataSource);
            this.priority = priority;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public RuleType getType() {
            return type;
        }

        public void setType(RuleType type) {
            this.type = type;
        }
    }

    /**
     * 规则类型
     */
    public enum RuleType {
        SPEL,           // SpEL表达式
        PARAMETER,      // 参数匹配
        HEADER,         // 请求头匹配
        TABLE_NAME,     // 表名匹配
        OPERATION_TYPE, // 操作类型匹配
        CUSTOM          // 自定义规则
    }

    @Override
    public boolean supports(RoutingContext context) {
        return hasRulesForTable(context.getTableName()) || hasGlobalRules();
    }

    @Override
    public String selectDataSource(RoutingContext context) {
        List<SelectionRule> rules = getAllApplicableRules(context);
        if (rules.isEmpty()) {
            return null;
        }

        // 按优先级排序
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        // 依次评估规则
        for (SelectionRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            try {
                if (evaluateRule(rule, context)) {
                    return resolveDataSource(rule.getDataSource(), context);
                }
            } catch (Exception e) {
                System.err.println("Error evaluating rule '" + rule.getName() + "': " + e.getMessage());
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return 100; // 较高优先级
    }

    /**
     * 添加选择规则
     */
    public void addRule(String tableName, SelectionRule rule) {
        ruleCache.computeIfAbsent(tableName, k -> new ArrayList<>()).add(rule);
    }

    /**
     * 添加全局规则
     */
    public void addGlobalRule(SelectionRule rule) {
        addRule("*", rule);
    }

    /**
     * 移除规则
     */
    public void removeRule(String tableName, String ruleName) {
        List<SelectionRule> rules = ruleCache.get(tableName);
        if (rules != null) {
            rules.removeIf(rule -> Objects.equals(rule.getName(), ruleName));
        }
    }

    /**
     * 清空规则
     */
    public void clearRules(String tableName) {
        ruleCache.remove(tableName);
    }

    /**
     * 清空所有规则
     */
    public void clearAllRules() {
        ruleCache.clear();
    }

    /**
     * 获取规则
     */
    public List<SelectionRule> getRules(String tableName) {
        return ruleCache.getOrDefault(tableName, Collections.emptyList());
    }

    /**
     * 检查是否有表级规则
     */
    private boolean hasRulesForTable(String tableName) {
        return ruleCache.containsKey(tableName) && !ruleCache.get(tableName).isEmpty();
    }

    /**
     * 检查是否有全局规则
     */
    private boolean hasGlobalRules() {
        return ruleCache.containsKey("*") && !ruleCache.get("*").isEmpty();
    }

    /**
     * 获取所有适用的规则
     */
    private List<SelectionRule> getAllApplicableRules(RoutingContext context) {
        List<SelectionRule> allRules = new ArrayList<>();
        
        // 添加表级规则
        String tableName = context.getTableName();
        if (StringUtils.hasText(tableName)) {
            List<SelectionRule> tableRules = ruleCache.get(tableName);
            if (tableRules != null) {
                allRules.addAll(tableRules);
            }
        }
        
        // 添加全局规则
        List<SelectionRule> globalRules = ruleCache.get("*");
        if (globalRules != null) {
            allRules.addAll(globalRules);
        }
        
        return allRules;
    }

    /**
     * 评估规则
     */
    private boolean evaluateRule(SelectionRule rule, RoutingContext context) {
        if (!StringUtils.hasText(rule.getCondition())) {
            return true; // 无条件规则总是匹配
        }

        switch (rule.getType()) {
            case SPEL:
                return evaluateSpelRule(rule, context);
            case PARAMETER:
                return evaluateParameterRule(rule, context);
            case HEADER:
                return evaluateHeaderRule(rule, context);
            case TABLE_NAME:
                return evaluateTableNameRule(rule, context);
            case OPERATION_TYPE:
                return evaluateOperationTypeRule(rule, context);
            case CUSTOM:
                return evaluateCustomRule(rule, context);
            default:
                return false;
        }
    }

    /**
     * 评估SpEL规则
     */
    private boolean evaluateSpelRule(SelectionRule rule, RoutingContext context) {
        if (spelEvaluator == null) {
            System.err.println("SpelExpressionEvaluator not available for rule: " + rule.getName());
            return false;
        }

        try {
            return spelEvaluator.evaluateCondition(rule.getCondition(), context);
        } catch (Exception e) {
            System.err.println("Error evaluating SpEL rule '" + rule.getName() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估参数规则
     */
    private boolean evaluateParameterRule(SelectionRule rule, RoutingContext context) {
        // 格式: paramName=value 或 paramName!=value 或 paramName>value 等
        String condition = rule.getCondition();
        
        if (condition.contains("=")) {
            String[] parts = condition.split("=", 2);
            if (parts.length == 2) {
                String paramName = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                if (paramName.endsWith("!")) {
                    // 不等于
                    paramName = paramName.substring(0, paramName.length() - 1);
                    Object actualValue = context.getParameter(paramName);
                    return !Objects.equals(String.valueOf(actualValue), expectedValue);
                } else {
                    // 等于
                    Object actualValue = context.getParameter(paramName);
                    return Objects.equals(String.valueOf(actualValue), expectedValue);
                }
            }
        }
        
        return false;
    }

    /**
     * 评估请求头规则
     */
    private boolean evaluateHeaderRule(SelectionRule rule, RoutingContext context) {
        // 格式: headerName=value
        String condition = rule.getCondition();
        
        if (condition.contains("=")) {
            String[] parts = condition.split("=", 2);
            if (parts.length == 2) {
                String headerName = parts[0].trim();
                String expectedValue = parts[1].trim();
                String actualValue = context.getHeader(headerName);
                return Objects.equals(actualValue, expectedValue);
            }
        }
        
        return false;
    }

    /**
     * 评估表名规则
     */
    private boolean evaluateTableNameRule(SelectionRule rule, RoutingContext context) {
        String condition = rule.getCondition();
        String tableName = context.getTableName();
        
        if (condition.startsWith("*") && condition.endsWith("*")) {
            // 包含匹配
            String pattern = condition.substring(1, condition.length() - 1);
            return tableName != null && tableName.contains(pattern);
        } else if (condition.startsWith("*")) {
            // 后缀匹配
            String suffix = condition.substring(1);
            return tableName != null && tableName.endsWith(suffix);
        } else if (condition.endsWith("*")) {
            // 前缀匹配
            String prefix = condition.substring(0, condition.length() - 1);
            return tableName != null && tableName.startsWith(prefix);
        } else {
            // 精确匹配
            return Objects.equals(tableName, condition);
        }
    }

    /**
     * 评估操作类型规则
     */
    private boolean evaluateOperationTypeRule(SelectionRule rule, RoutingContext context) {
        String condition = rule.getCondition().toUpperCase();
        RoutingContext.OperationType operationType = context.getOperationType();
        
        if (operationType == null) {
            return false;
        }
        
        switch (condition) {
            case "READ":
            case "SELECT":
                return context.isReadOperation();
            case "WRITE":
                return context.isWriteOperation();
            case "BATCH":
                return context.isBatchOperation();
            default:
                return operationType.name().equals(condition);
        }
    }

    /**
     * 评估自定义规则
     */
    private boolean evaluateCustomRule(SelectionRule rule, RoutingContext context) {
        // 可以扩展实现自定义规则逻辑
        // 这里提供一个简单的示例实现
        return true;
    }

    /**
     * 解析数据源
     */
    private String resolveDataSource(String dataSourceExpression, RoutingContext context) {
        if (!StringUtils.hasText(dataSourceExpression)) {
            return null;
        }

        // 如果是SpEL表达式
        if (dataSourceExpression.startsWith("#{") && dataSourceExpression.endsWith("}") && spelEvaluator != null) {
            try {
                return spelEvaluator.evaluateExpression(dataSourceExpression, context, String.class);
            } catch (Exception e) {
                System.err.println("Error evaluating dataSource expression: " + e.getMessage());
                return null;
            }
        }

        return dataSourceExpression;
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private RuleBasedDataSourceSelector selector = new RuleBasedDataSourceSelector();

        public Builder addRule(String tableName, String ruleName, String condition, String dataSource) {
            SelectionRule rule = new SelectionRule(ruleName, condition, dataSource);
            selector.addRule(tableName, rule);
            return this;
        }

        public Builder addRule(String tableName, String ruleName, String condition, String dataSource, int priority) {
            SelectionRule rule = new SelectionRule(ruleName, condition, dataSource, priority);
            selector.addRule(tableName, rule);
            return this;
        }

        public Builder addGlobalRule(String ruleName, String condition, String dataSource) {
            SelectionRule rule = new SelectionRule(ruleName, condition, dataSource);
            selector.addGlobalRule(rule);
            return this;
        }

        public Builder addGlobalRule(String ruleName, String condition, String dataSource, int priority) {
            SelectionRule rule = new SelectionRule(ruleName, condition, dataSource, priority);
            selector.addGlobalRule(rule);
            return this;
        }

        public RuleBasedDataSourceSelector build() {
            return selector;
        }
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
}