# Database Backup Tool

[![Build Status](https://cloud.drone.io/api/badges/blinky-z/DbBackupTool/status.svg)](https://cloud.drone.io/blinky-z/DbBackupTool)

## Introduction
The primary goal of this project is to make it easier to backup/restore various databases, upload/download backups
 and manage your backups using a single app.

## Features
* Supports PostgreSQL database for now
* Supports Local File System, Dropbox storage for now
* Scheduled backup tasks
* Support for running in Docker
* Supports running more than one copy of program
* Includes WebUI to access app API

## Quick start
1. Open *docker-compose.yml* and set the following environment variables:
 - *SPRING_DATASOURCE_URL* - url to access your database. Must look like: `jdbc:postgresql://{host}:{port}/{dbname}`. Database is required to store backups meta data.
 - *SPRING_DATASOURCE_USERNAME* - database username
 - *SPRING_DATASOURCE_PASSWORD* - database password
 - *USERCONFIG_WEBUILOGIN* - WebUI login
 - *USERCONFIG_WEBUIPASSWORD* - WebUI password
3. Run the app: ```docker-compose up -d```
4. Now you can access Web UI in your browser by url `localhost:8080`.