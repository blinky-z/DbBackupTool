# Database Backup Tool

## Introduction
The primary goal of this project is to make it easier to backup/restore various databases, upload/download backups
 and manage your backups using a single app.

## Features
* Supports PostgreSQL database for now
* Supports Local File System, Dropbox storage for now
* Scheduled backup tasks
* Support for running in Docker
* Supports running more that one copy of program
* Includes WebUI protected with login & password to access app API

## Quick start
1. Create PostgreSQL database for storing of app settings and backups information. All necessary tables will be created and initialized 
with flyway migration when the app starts.
2. Open *docker-compose.yml* and set the following fields:
 - *spring.datasource.url* - url to access your database. Must look like: jdbc:postgresql://{host}:{port}/{dbname}"
 - *spring.datasource.username* - database username
 - *spring.datasource.password* - database password
 - *user-config.webUILogin* - WebUI login
 - *user-config.webUIPassword* - WebUI password
3. Run the app using Docker Compose: ```docker-compose up```
4. Now you access Web UI in your browser by url `localhost:8080`.

## Running tests
1. Open class *test/resources/TestConfiguration* and set the following fields:
    - *dropboxAccessToken* - Dropbox storage for uploading backup while testing of Dropbox storage