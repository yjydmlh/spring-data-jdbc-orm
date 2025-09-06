package io.flexdata.spring.orm.core.sql;

import io.flexdata.spring.orm.core.table.TableContext;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL表名动态替换器
 * 根据TableContext中的表名映射动态替换SQL中的表名
 */
public class SqlTableReplacer {
    
    // 匹配SQL中表名的正则表达式
    // 支持 FROM table_name, JOIN table_name, UPDATE table_name, INSERT INTO table_name 等
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "\\b(?i)(FROM|JOIN|UPDATE|INSERT\\s+INTO|INTO)\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配带别名的表名
    private static final Pattern TABLE_WITH_ALIAS_PATTERN = Pattern.compile(
        "\\b(?i)(FROM|JOIN|UPDATE|INSERT\\s+INTO|INTO)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s+(AS\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );
    
    // 匹配DELETE FROM语句
    private static final Pattern DELETE_PATTERN = Pattern.compile(
        "\\b(?i)DELETE\\s+FROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 替换SQL中的表名
     * @param sql 原始SQL
     * @return 替换后的SQL
     */
    public static String replaceTableNames(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }
        
        Map<String, String> tableMappings = TableContext.getAllTableMappings();
        if (tableMappings.isEmpty()) {
            return sql;
        }
        
        String result = sql;
        
        // 替换基本的表名
        result = replaceBasicTableNames(result, tableMappings);
        
        // 替换带别名的表名
        result = replaceTableNamesWithAlias(result, tableMappings);
        
        // 替换DELETE语句中的表名
        result = replaceDeleteTableNames(result, tableMappings);
        
        return result;
    }
    
    /**
     * 替换基本的表名（不带别名）
     */
    private static String replaceBasicTableNames(String sql, Map<String, String> tableMappings) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String keyword = matcher.group(1);
            String tableName = matcher.group(2);
            
            // 检查是否需要替换
            String physicalTableName = tableMappings.get(tableName);
            if (physicalTableName != null) {
                matcher.appendReplacement(sb, keyword + " " + physicalTableName);
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 替换带别名的表名
     */
    private static String replaceTableNamesWithAlias(String sql, Map<String, String> tableMappings) {
        Matcher matcher = TABLE_WITH_ALIAS_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String keyword = matcher.group(1);
            String tableName = matcher.group(2);
            String asKeyword = matcher.group(3); // AS关键字（可能为null）
            String alias = matcher.group(4);
            
            // 检查是否需要替换
            String physicalTableName = tableMappings.get(tableName);
            if (physicalTableName != null) {
                String replacement = keyword + " " + physicalTableName;
                if (StringUtils.hasText(asKeyword)) {
                    replacement += " " + asKeyword + alias;
                } else {
                    replacement += " " + alias;
                }
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 替换DELETE语句中的表名
     */
    private static String replaceDeleteTableNames(String sql, Map<String, String> tableMappings) {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            
            // 检查是否需要替换
            String physicalTableName = tableMappings.get(tableName);
            if (physicalTableName != null) {
                matcher.appendReplacement(sb, "DELETE FROM " + physicalTableName);
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 替换指定表名
     * @param sql 原始SQL
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 替换后的SQL
     */
    public static String replaceTableName(String sql, String logicalTableName, String physicalTableName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(logicalTableName) || !StringUtils.hasText(physicalTableName)) {
            return sql;
        }
        
        // 创建临时映射
        Map<String, String> tempMapping = Map.of(logicalTableName, physicalTableName);
        
        String result = sql;
        result = replaceBasicTableNames(result, tempMapping);
        result = replaceTableNamesWithAlias(result, tempMapping);
        result = replaceDeleteTableNames(result, tempMapping);
        
        return result;
    }
    
    /**
     * 批量替换表名
     * @param sql 原始SQL
     * @param tableMappings 表名映射
     * @return 替换后的SQL
     */
    public static String replaceTableNames(String sql, Map<String, String> tableMappings) {
        if (!StringUtils.hasText(sql) || tableMappings == null || tableMappings.isEmpty()) {
            return sql;
        }
        
        String result = sql;
        result = replaceBasicTableNames(result, tableMappings);
        result = replaceTableNamesWithAlias(result, tableMappings);
        result = replaceDeleteTableNames(result, tableMappings);
        
        return result;
    }
    
    /**
     * 检查SQL是否包含指定的表名
     * @param sql SQL语句
     * @param tableName 表名
     * @return 是否包含
     */
    public static boolean containsTable(String sql, String tableName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(tableName)) {
            return false;
        }
        
        // 创建匹配指定表名的正则表达式
        String pattern = "\\b(?i)(FROM|JOIN|UPDATE|INSERT\\s+INTO|INTO|DELETE\\s+FROM)\\s+" + 
                        Pattern.quote(tableName) + "\\b";
        Pattern tablePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        
        return tablePattern.matcher(sql).find();
    }
    
    /**
     * 提取SQL中的所有表名
     * @param sql SQL语句
     * @return 表名集合
     */
    public static java.util.Set<String> extractTableNames(String sql) {
        java.util.Set<String> tableNames = new java.util.HashSet<>();
        
        if (!StringUtils.hasText(sql)) {
            return tableNames;
        }
        
        // 匹配基本表名
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            tableNames.add(matcher.group(2));
        }
        
        // 匹配带别名的表名
        Matcher aliasMatcher = TABLE_WITH_ALIAS_PATTERN.matcher(sql);
        while (aliasMatcher.find()) {
            tableNames.add(aliasMatcher.group(2));
        }
        
        // 匹配DELETE语句中的表名
        Matcher deleteMatcher = DELETE_PATTERN.matcher(sql);
        while (deleteMatcher.find()) {
            tableNames.add(deleteMatcher.group(1));
        }
        
        return tableNames;
    }
    
    /**
     * 验证表名映射是否有效
     * @param sql SQL语句
     * @param tableMappings 表名映射
     * @return 验证结果
     */
    public static ValidationResult validateTableMappings(String sql, Map<String, String> tableMappings) {
        ValidationResult result = new ValidationResult();
        
        if (!StringUtils.hasText(sql)) {
            result.setValid(true);
            return result;
        }
        
        java.util.Set<String> sqlTables = extractTableNames(sql);
        java.util.Set<String> mappingTables = tableMappings.keySet();
        
        // 检查SQL中的表是否都有映射
        for (String table : sqlTables) {
            if (!mappingTables.contains(table)) {
                result.addMissingMapping(table);
            }
        }
        
        // 检查映射中是否有多余的表
        for (String table : mappingTables) {
            if (!sqlTables.contains(table)) {
                result.addUnusedMapping(table);
            }
        }
        
        result.setValid(result.getMissingMappings().isEmpty());
        return result;
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private java.util.List<String> missingMappings = new java.util.ArrayList<>();
        private java.util.List<String> unusedMappings = new java.util.ArrayList<>();
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public java.util.List<String> getMissingMappings() { return missingMappings; }
        public void addMissingMapping(String table) { this.missingMappings.add(table); }
        
        public java.util.List<String> getUnusedMappings() { return unusedMappings; }
        public void addUnusedMapping(String table) { this.unusedMappings.add(table); }
    }
}