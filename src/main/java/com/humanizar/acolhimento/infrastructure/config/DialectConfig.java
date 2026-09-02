package com.humanizar.acolhimento.infrastructure.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import com.humanizar.acolhimento.infrastructure.persistence.dialect.NoEnumCheckPostgreSQLDialect;

@Configuration
@ImportRuntimeHints(DialectConfig.DialectRuntimeHints.class)
public class DialectConfig {

    public static class DialectRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(NoEnumCheckPostgreSQLDialect.class, MemberCategory.values());
        }
    }
}
