FROM twite/db-backup-tool-base:0.0.1
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
COPY --chown=gradle:gradle . .
RUN gradle build -x test
ENTRYPOINT ["java", "-jar", "/usr/app/build/libs/DatabaseBackupTool-0.0.1-SNAPSHOT.jar"]