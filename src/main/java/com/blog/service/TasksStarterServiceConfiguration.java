package com.blog.service;

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class TasksStarterServiceConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService tasksStarterExecutorService() {
        return Executors.newFixedThreadPool(100);
    }

    @Bean
    @Autowired
    public JdbcTemplateLockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
