# Drug Tracker Web Application

A full-stack web application for managing pharmaceutical compounds,
clinical trials, and laboratory inventory.

Built with: - Kotlin - Spring Boot 3 - Spring Data JPA (Hibernate) -
Thymeleaf - MySQL - Gradle

------------------------------------------------------------------------

# What This Project Does

This application allows users to:

-   Create and manage drug compounds\
-   Register clinical trials\
-   Track laboratory inventory\
-   Enforce validation rules\
-   Maintain relationships between compounds, trials, and inventory

Architecture:

Controller → Service → Repository → Database

------------------------------------------------------------------------

# System Requirements

Before running this project, install:

1.  Java 17 or newer\
2.  Git\
3.  MySQL 8+\
4.  (Recommended) IntelliJ IDEA

------------------------------------------------------------------------

# Installing Java

Check if installed:

    java -version

If not installed, download from: https://adoptium.net/

Install Java 17 and verify installation again.

------------------------------------------------------------------------

# Installing Git

Check:

    git --version

Download if needed: https://git-scm.com/

------------------------------------------------------------------------

# Installing MySQL

Download: https://dev.mysql.com/downloads/mysql/

After installation:

    mysql --version

------------------------------------------------------------------------

# Cloning The Project

Open terminal:

    git clone https://github.com/selinece17/drugtracker.git
    cd drugtracker

----------------------------------------------------------------------

# Database Setup

Start MySQL:

Mac (Homebrew): brew services start mysql

Or: mysql.server start

Login:

    mysql -u root -p

Inside MySQL:

    CREATE DATABASE drugtracker;

    CREATE USER 'druguser'@'localhost' IDENTIFIED BY 'password';

    GRANT ALL PRIVILEGES ON drugtracker.* TO 'druguser'@'localhost';

    FLUSH PRIVILEGES;

    EXIT;

------------------------------------------------------------------------

# Application Configuration

Open:

    src/main/resources/application.properties

Ensure it contains:

    spring.datasource.url=jdbc:mysql://localhost:3306/drugtracker
    spring.datasource.username=druguser
    spring.datasource.password=password
    spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true

    spring.thymeleaf.cache=false

Adjust password if needed.

------------------------------------------------------------------------

# Installing Dependencies

From project root:

    ./gradlew build

Windows:

    gradlew build

------------------------------------------------------------------------

# Running The Application

From root directory:

    ./gradlew bootRun

If successful:

    Tomcat started on port 8080

------------------------------------------------------------------------

# Accessing The Application

Open browser:

    http://localhost:8080

Main pages:

    /compounds
    /trials
    /inventory

------------------------------------------------------------------------

# Running Tests

Run all tests:

    ./gradlew test

Test report:

    build/reports/tests/test/index.html

------------------------------------------------------------------------

# Project Structure

src/ ├── main/ │ ├── kotlin/com/drugtracker/ │ │ ├── controller/ │ │ ├──
service/ │ │ ├── repository/ │ │ ├── model/ │ │ └──
DrugTrackerApplication.kt │ └── resources/ │ ├── templates/ │ └──
application.properties └── test/ └── kotlin/com/drugtracker/

------------------------------------------------------------------------

# Validation Rules

-   Compound name must be unique\
-   Trial ID must be unique\
-   End date must be after start date\
-   Inventory lot number must be unique\
-   Received date cannot be in the future

------------------------------------------------------------------------

# Common Errors

Port 8080 already in use:

    lsof -i :8080
    kill -9 <PID>

MySQL connection errors: - Ensure MySQL is running - Ensure database
exists - Ensure credentials match

------------------------------------------------------------------------

# Technologies Used

-   Spring Boot 3.2
-   Kotlin 1.9
-   Hibernate ORM
-   Thymeleaf
-   MySQL 8+
-   Gradle 8+
-   JUnit 5

------------------------------------------------------------------------

# License

Educational and academic use.
