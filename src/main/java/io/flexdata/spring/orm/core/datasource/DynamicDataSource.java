package io.flexdata.spring.orm.core.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;

/**
 * 动态数据源路由器
 * 根据当前线程上下文动态选择数据源
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    public static final String DEFAULT_DATASOURCE = "default";
    
    @Nullable
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceKey = DataSourceContext.getDataSource();
        return dataSourceKey != null ? dataSourceKey : DEFAULT_DATASOURCE;
    }
    
    @Override
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        return lookupKey;
    }
}