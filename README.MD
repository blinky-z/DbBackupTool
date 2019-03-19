# Database Backup Tool

Приложение для создания и восстановления бэкапов.

## Быстрый старт
1. Создайте базу данных Postgres для хранения необходимых конфигураций приложения.
2. Исполните на своей базе данных скрипт **init.sql**, который вы найдете в корневой папке проекта в папке *sqlScripts*
3. Откройте application.properties и установите следующие поля:
 - spring.datasource.url
 - spring.datasource.username
 - spring.datasource.password
 - user-config.database-name
 - user-config.webUILogin
 - user-config.webUIPassword
 
Пример конфигурации:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
user-config.database-name=postgres
user-config.webUILogin=qwerty
user-config.webUIPassword=qwerty
```
4. Соберите и запустите приложение.
5. Перейдите на адрес сервера Spring Boot, по умолчанию - *localhost:8080*.