FROM gradle:5.4.1-jre11
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME
USER root
RUN echo 'deb http://apt.postgresql.org/pub/repos/apt/ stretch-pgdg main' > /etc/apt/sources.list.d/pgdg.list
RUN wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client-10
COPY . .
RUN chown -R gradle $APP_HOME
USER gradle
RUN gradle build
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/app/build/libs/DatabaseBackupTool-0.0.1-SNAPSHOT.jar"]