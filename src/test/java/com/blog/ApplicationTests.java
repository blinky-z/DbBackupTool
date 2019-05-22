package com.blog;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.flywaydb.test.annotation.FlywayTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {Application.class, TestsConfiguration.class})
@FlywayTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource") // replace standard data source
@AutoConfigureEmbeddedDatabase(beanName = "masterPostgresDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyPostgresDataSource")
public abstract class ApplicationTests {
}
