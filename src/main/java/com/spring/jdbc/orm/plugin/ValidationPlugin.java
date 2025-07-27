package com.spring.jdbc.orm.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

/**
 * 数据验证插件
 */
@Component
@ConditionalOnProperty(name = "orm.plugin.validation.enabled", havingValue = "true")
public class ValidationPlugin implements OrmPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ValidationPlugin.class);

    private final Validator validator;
    private volatile boolean enabled = true;

    public ValidationPlugin(Validator validator) {
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "Validation";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "实体数据验证";
    }

    @Override
    public void initialize() {
        logger.info("数据验证插件已启动");
    }

    @Override
    public void destroy() {
        logger.info("数据验证插件已销毁");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void beforeEntitySave(Object entity) {
        if (validator != null) {
            Set<ConstraintViolation<Object>> violations = validator.validate(entity);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder("实体验证失败: ");
                violations.forEach(v -> sb.append(v.getPropertyPath()).append(" ").append(v.getMessage()).append("; "));
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }
}
