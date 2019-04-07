# Database Backup Tool

Приложение для создания и восстановления бэкапов.

## Быстрый старт
1. Создайте базу данных Postgres для хранения необходимых конфигураций приложения.
2. Откройте *application.properties* и установите следующие поля:
 - *spring.datasource.url*
 - *spring.datasource.username*
 - *spring.datasource.password*
 - *user-config.webUILogin*
 - *user-config.webUIPassword*
 
Пример конфигурации:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
user-config.webUILogin=qwerty
user-config.webUIPassword=qwerty
```
3. Соберите и запустите приложение, выполнив Gradle команду - application/bootRun
4. Перейдите на адрес сервера Spring Boot, по умолчанию - *localhost:8080*.

## Запуск тестов
1. Откройте класс *TestConfiguration* и установите следующие поля:
    - dropboxAccessToken - Dropbox хранилище куда будут загружаться бэкапы созданные во время тестов
4. Запустите тесты, выполнив Gradle команду - *verification/test*