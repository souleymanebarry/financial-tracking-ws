package com.barry.bank.document.freemarker;

import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class FreemarkerDocumentConfig {

    @Bean
    public freemarker.template.Configuration freemarkerDocumentConfiguration() {
        var config = new freemarker.template.Configuration(
                freemarker.template.Configuration.VERSION_2_3_32);
        config.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), "templates");
        config.setDefaultEncoding("UTF-8");
        config.setLocale(Locale.FRENCH);
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        return config;
    }
}