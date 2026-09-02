package com.humanizar.acolhimento.infrastructure.persistence.dialect;

import java.util.Collection;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class NoEnumCheckPostgreSQLDialect extends PostgreSQLDialect {

    public NoEnumCheckPostgreSQLDialect() {
    }

    public NoEnumCheckPostgreSQLDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    public String getCheckCondition(String columnName, String[] values) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, Class<? extends Enum<?>> enumType) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, long min, long max) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, long[] values) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, Long[] values) {
        return null;
    }

    @Override
    public String getCheckCondition(String columnName, Collection<?> valueSet, JdbcType jdbcType) {
        return null;
    }
}
