package io.flexdata.spring.orm.routing.selector;

import io.flexdata.spring.orm.routing.context.RoutingContext;

/**
 * 数据源选择器接口
 * 用于实现基于规则的数据源选择逻辑
 */
public interface DataSourceSelector {

    /**
     * 检查是否支持当前上下文
     * 
     * @param context 路由上下文
     * @return 是否支持
     */
    boolean supports(RoutingContext context);

    /**
     * 选择数据源
     * 
     * @param context 路由上下文
     * @return 数据源名称
     */
    String selectDataSource(RoutingContext context);

    /**
     * 获取选择器优先级
     * 数值越大优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 获取选择器名称
     * 
     * @return 选择器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}