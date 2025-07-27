package com.spring.jdbc.orm.core.util;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化的函数接口，用于Lambda表达式
 * 文件位置: src/main/java/com/example/orm/core/util/SFunction.java
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
