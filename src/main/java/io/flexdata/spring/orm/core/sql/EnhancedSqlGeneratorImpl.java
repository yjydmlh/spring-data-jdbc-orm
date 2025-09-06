package io.flexdata.spring.orm.core.sql;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.sql.complex.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强的SQL生成器实现
 * 支持复杂SQL语法：多表JOIN、子查询、聚合函数、窗口函数、UNION等
 */
@Component
public class EnhancedSqlGeneratorImpl implements EnhancedSqlGenerator {
    
    private final EntityMetadataRegistry metadataRegistry;
    private final SqlGenerator basicSqlGenerator;
    
    public EnhancedSqlGeneratorImpl(EntityMetadataRegistry metadataRegistry, SqlGenerator basicSqlGenerator) {
        this.metadataRegistry = metadataRegistry;
        this.basicSqlGenerator = basicSqlGenerator;
    }
    
    @Override
    public String generateComplexSelect(ComplexSelectQuery query) {
        StringBuilder sql = new StringBuilder();
        
        // CTE部分
        if (!query.getCteDefinitions().isEmpty()) {
            sql.append("WITH ");
            for (int i = 0; i < query.getCteDefinitions().size(); i++) {
                if (i > 0) sql.append(", ");
                CteDefinition cte = query.getCteDefinitions().get(i);
                sql.append(cte.getName()).append(" AS (");
                sql.append(generateSubQuery(cte.getQuery()));
                sql.append(")");
            }
            sql.append(" ");
        }
        
        // SELECT部分
        sql.append("SELECT ");
        if (query.isDistinct()) {
            sql.append("DISTINCT ");
        }
        
        if (query.getSelectFields().isEmpty()) {
            sql.append("*");
        } else {
            sql.append(query.getSelectFields().stream()
                .map(this::buildSelectField)
                .collect(Collectors.joining(", ")));
        }
        
        // FROM部分
        if (!query.getFromTables().isEmpty()) {
            sql.append(" FROM ");
            sql.append(query.getFromTables().stream()
                .map(this::buildTableReference)
                .collect(Collectors.joining(", ")));
        }
        
        // JOIN部分
        for (JoinClause join : query.getJoins()) {
            sql.append(" ").append(join.getJoinType().getSql()).append(" ");
            if (join.isSubQuery()) {
                sql.append("(").append(generateSubQuery(join.getSubQuery())).append(")");
            } else {
                sql.append(join.getTableName());
            }
            if (join.getAlias() != null) {
                sql.append(" ").append(join.getAlias());
            }
            sql.append(" ON ").append(join.getOnCondition());
        }
        
        // WHERE部分
        if (query.getWhereClause() != null) {
            Map<String, String> aliasMap = buildTableAliasMap(query.getFromTables());
            sql.append(" WHERE ").append(parseComplexWhere(query.getWhereClause(), aliasMap));
        }
        
        // GROUP BY部分
        if (!query.getGroupByFields().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", query.getGroupByFields()));
        }
        
        // HAVING部分
        if (query.getHavingClause() != null) {
            Map<String, String> aliasMap = buildTableAliasMap(query.getFromTables());
            sql.append(" HAVING ").append(parseComplexWhere(query.getHavingClause(), aliasMap));
        }
        
        // ORDER BY部分
        if (!query.getOrderByFields().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(query.getOrderByFields().stream()
                .map(order -> order.getField() + " " + order.getDirection())
                .collect(Collectors.joining(", ")));
        }
        
        // LIMIT和OFFSET
        if (query.getLimit() != null) {
            sql.append(" LIMIT ").append(query.getLimit());
        }
        if (query.getOffset() != null) {
            sql.append(" OFFSET ").append(query.getOffset());
        }
        
        return sql.toString();
    }
    
    @Override
    public String generateJoinQuery(JoinQuery joinQuery) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        
        // 主表
        if (!joinQuery.getTables().isEmpty()) {
            sql.append(buildTableReference(joinQuery.getTables().get(0)));
        }
        
        // JOIN子句
        for (JoinClause join : joinQuery.getJoins()) {
            sql.append(" ").append(join.getJoinType().getSql()).append(" ");
            if (join.isSubQuery()) {
                sql.append("(").append(generateSubQuery(join.getSubQuery())).append(")");
            } else {
                sql.append(join.getTableName());
            }
            if (join.getAlias() != null) {
                sql.append(" ").append(join.getAlias());
            }
            sql.append(" ON ").append(join.getOnCondition());
        }
        
        // WHERE条件
        if (joinQuery.getWhereClause() != null) {
            Map<String, String> aliasMap = buildTableAliasMap(joinQuery.getTables());
            sql.append(" WHERE ").append(parseComplexWhere(joinQuery.getWhereClause(), aliasMap));
        }
        
        return sql.toString();
    }
    
    @Override
    public String generateSubQuery(SubQuery subQuery) {
        return generateComplexSelect(subQuery.getSelectQuery());
    }
    
    @Override
    public String generateAggregateQuery(AggregateQuery aggregateQuery) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // 聚合函数
        if (!aggregateQuery.getAggregateFunctions().isEmpty()) {
            sql.append(aggregateQuery.getAggregateFunctions().stream()
                .map(AggregateFunction::toSql)
                .collect(Collectors.joining(", ")));
        } else {
            sql.append("*");
        }
        
        // FROM表
        sql.append(" FROM ").append(buildTableReference(aggregateQuery.getFromTable()));
        
        // GROUP BY
        if (!aggregateQuery.getGroupByFields().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", aggregateQuery.getGroupByFields()));
        }
        
        // HAVING
        if (aggregateQuery.getHavingClause() != null) {
            sql.append(" HAVING ").append(aggregateQuery.getHavingClause().toSql());
        }
        
        return sql.toString();
    }
    
    @Override
    public String generateWindowFunctionQuery(WindowFunctionQuery windowQuery) {
        return windowQuery.toSql(new HashMap<>());
    }
    
    @Override
    public String generateUnionQuery(UnionQuery unionQuery) {
        return unionQuery.toSql(new HashMap<>());
    }
    
    @Override
    public String generateCteQuery(CteQuery cteQuery) {
        return cteQuery.toSql(new HashMap<>());
    }
    
    @Override
    public String generateExistsQuery(ExistsQuery existsQuery) {
        return existsQuery.toSql(new HashMap<>());
    }
    
    @Override
    public String generateCaseWhenExpression(CaseWhenExpression caseWhen) {
        return caseWhen.toSql(new HashMap<>());
    }
    
    @Override
    public Map<String, String> buildTableAliasMap(List<TableReference> tables) {
        Map<String, String> aliasMap = new HashMap<>();
        for (TableReference table : tables) {
            if (table.getAlias() != null) {
                aliasMap.put(table.getAlias(), table.getTableName());
            }
        }
        return aliasMap;
    }
    
    @Override
    public String parseComplexWhere(Criteria criteria, Map<String, String> aliasMap) {
        String sql = criteria.toSql();
        
        // 处理表别名替换
        for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
            String alias = entry.getKey();
            String tableName = entry.getValue();
            // 替换 alias.field 为实际的列名
            sql = sql.replaceAll("\\b" + alias + "\\.(\\w+)", alias + ".$1");
        }
        
        return sql;
    }
    
    @Override
    public String generateDynamicFragment(SqlFragment fragment, Map<String, Object> context) {
        return fragment.toSql(context);
    }
    
    // 私有辅助方法
    private String buildSelectField(SelectField field) {
        switch (field.getType()) {
            case SIMPLE:
                return field.getFieldExpression();
            case EXPRESSION:
                String expr = field.getFieldExpression();
                return field.getAlias() != null ? expr + " AS " + field.getAlias() : expr;
            case SUBQUERY:
                String subSql = "(" + generateSubQuery(field.getSubQuery()) + ")";
                return field.getAlias() != null ? subSql + " AS " + field.getAlias() : subSql;
            case AGGREGATE:
                String aggSql = field.getAggregateFunction().toSql();
                return field.getAlias() != null ? aggSql + " AS " + field.getAlias() : aggSql;
            default:
                return field.getFieldExpression();
        }
    }
    
    private String buildTableReference(TableReference table) {
        if (table.isSubQuery()) {
            String subSql = "(" + generateSubQuery(table.getSubQuery()) + ")";
            return table.getAlias() != null ? subSql + " " + table.getAlias() : subSql;
        } else {
            return table.getAlias() != null ? 
                table.getTableName() + " " + table.getAlias() : 
                table.getTableName();
        }
    }
}