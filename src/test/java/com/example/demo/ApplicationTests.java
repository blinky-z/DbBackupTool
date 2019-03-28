package com.example.demo;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApplication.class, TestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FlywayTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource") // replace standard data source
@AutoConfigureEmbeddedDatabase(beanName = "masterDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyDataSource")
public abstract class ApplicationTests {
}
