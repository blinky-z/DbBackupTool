package com.example.demo;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DemoApplication.class, TestConfiguration.class})
@FlywayTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource") // replace standard data source
@AutoConfigureEmbeddedDatabase(beanName = "masterPostgresDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyPostgresDataSource")
public abstract class ApplicationTests {
}
