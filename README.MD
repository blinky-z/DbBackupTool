# Database Backup Tool

## Introduction
The primary goal of this project is to make it easier to backup/restore various databases, upload/download backups
 and manage your backups using single app.

## Features
* Supports PostgreSQL database for now
* Supports Local File System, Dropbox storages for now
* Scheduled tasks
    * Scheduled backup task
* Support for running in Docker
* Includes WebUI protected with login & password to access app API

## Quick start
1. Create PostgreSQL database to store app settings and backups information. All necessary tables will be created and initialized 
with flyway migration when the app starts
2. Open *application.properties* and set the following fields:
 - *spring.datasource.url* - url to access your database. Must be look like: jdbc:postgresql://{host}:{port}/{dbname}"
 - *spring.datasource.username* - database username
 - *spring.datasource.password* - database password
 - *user-config.webUILogin* - WebUI login
 - *user-config.webUIPassword* - WebUI password
 
Configuration example:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
user-config.webUILogin=qwerty
user-config.webUIPassword=qwerty
```
3. Run the app and open Tomcat server (default url: localhost:8080)

## Running tests
1. Open class *test/resources/TestConfiguration* and set the following fields:
    - *dropboxAccessToken* - Dropbox storage for uploading backup while testing of Dropbox storage