package com.blog;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {TestsConfiguration.class})
@FlywayTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource") // replace standard data source
@AutoConfigureEmbeddedDatabase(beanName = "masterPostgresDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyPostgresDataSource")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApplicationTests {
}
