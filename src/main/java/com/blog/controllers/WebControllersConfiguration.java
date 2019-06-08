package com.blog.controllers;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.format.DateTimeFormatter;

@Configuration
public class WebControllersConfiguration {
    /**
     * This time format is used in all elements of front-end (backup creation time, task start time and etc).
     */
    private static final String WEB_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    @Bean
    public DateTimeFormatter webDateFormatter() {
        return DateTimeFormatter.ofPattern(WEB_TIME_FORMAT);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource
                = new ReloadableResourceBundleMessageSource();

        messageSource.setBasename("classpath:validation-messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}
