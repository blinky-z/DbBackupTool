package com.example.demo.controllers.WebApi;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.manager.DatabaseSettingsManager;
import com.example.demo.webUI.formTransfer.WebAddDatabaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/database")
public class WebApiDatabaseController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiDatabaseController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @DeleteMapping
    public String deleteDatabase(@RequestParam(value = "id") int id) {
        logger.info("deleteDatabase(): Got database configuration deletion job. Database ID: {}", id);

        databaseSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    @PostMapping
    public String createDatabase(@Valid WebAddDatabaseRequest createDatabaseRequest) {
        logger.info("createDatabase(): Got database configuration creation job");

        Optional<Database> databaseType = Database.of(createDatabaseRequest.getDatabaseType());
        if (databaseType.isPresent()) {
            switch (databaseType.get()) {
                case POSTGRES: {
                    PostgresSettings postgresSettings = new PostgresSettings();

                    DatabaseSettings databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(createDatabaseRequest.getHost())
                            .withPort(Integer.valueOf(createDatabaseRequest.getPort()))
                            .withName(createDatabaseRequest.getName())
                            .withLogin(createDatabaseRequest.getLogin())
                            .withPassword(createDatabaseRequest.getPassword())
                            .build();
                    databaseSettingsManager.save(databaseSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create database configuration. Error: Unknown database type");
        }


        return "redirect:/dashboard";
    }
}
