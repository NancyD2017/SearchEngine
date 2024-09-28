<b>SearchEngine</b>
![image](https://github.com/user-attachments/assets/b3df694a-beac-42ac-b869-1a5a9644f8f9)
## Описание проекта

Данный проект представляет собой поисковый движок, реализованный в виде Spring-приложения. Основные функциональные возможности:

- Индексация веб-страниц, хранящихся в локальной базе данных MySQL.
![image](https://github.com/user-attachments/assets/855a1c87-aef2-4774-b9ec-64c862e442c2)
- Поиск по индексированным страницам с возможностью фильтрации по сайту.
![image](https://github.com/user-attachments/assets/93a0d78c-7b2e-4cc3-aa51-4f82fdf2a610)
- Наличие REST API для управления и получения результатов поиска.

## Стек используемых технологий

- Java 11
- Spring Boot
- Spring Data JPA
- MySQL
- Hibernate
- Thymeleaf (для веб-интерфейса)

## Инструкция по локальному запуску

1. Установите и настройте MySQL сервер на своем компьютере.
2. Создайте базу данных для проекта.
3. Склонируйте репозиторий проекта:
   
   git clone https://github.com/NancyD2017/SearchEngine.git
   
4. Перейдите в директорию проекта:
   
   cd search-engine
   
5. Откройте проект в вашей IDE (например, IntelliJ IDEA или Eclipse).
6. Настройте подключение к базе данных MySQL в файле application.properties:
   
   spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
7. Соберите проект с помощью Maven:
   
   mvn clean install
   
8. Запустите приложение:
   
   java -jar target/search-engine-0.0.1-SNAPSHOT.jar
   
9. Откройте веб-интерфейс в браузере по адресу http://localhost:8080.
10. Используйте REST API, доступное по адресу http://localhost:8080/api.
