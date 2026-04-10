# ReWear

ReWear is a Java Swing desktop application for community clothing exchange. It uses MySQL for persistence and follows a layered structure with UI, DAO, model, and utility components.

## Features

- User authentication: login and registration
- Item listing and browsing
- Requesting and managing exchanges
- Chat support for exchange conversations
- Review submission after completed exchanges
- Points-based system for item exchange

## Project Structure

- `src/main/java/com/rewear/` - main application classes
- `src/main/java/com/rewear/ui/` - Swing UI frames
- `src/main/java/com/rewear/dao/` - database access objects
- `src/main/java/com/rewear/models/` - domain models
- `src/main/java/com/rewear/exceptions/` - custom exceptions
- `src/main/java/com/rewear/validators/` - input validation logic
- `src/main/java/com/rewear/models/sql/schema.sql` - MySQL schema

## Prerequisites

- Java JDK 17 or newer
- Maven
- MySQL database

## Database Setup

1. Create the database and tables using the schema file:

```bash
mysql -u root -p < src/main/java/com/rewear/models/sql/schema.sql
```

2. Update JDBC connection settings in `src/main/java/com/rewear/DBConnection.java` if needed:

- `rewear.jdbc.url`
- `rewear.jdbc.user`
- `rewear.jdbc.password`

The default connection string is:

```java
jdbc:mysql://localhost:3306/rewear?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

## Build and Run

From the project root:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.rewear.Main"
```

Or, build a jar and run:

```bash
mvn package
java -cp target/classes;target/dependency/* com.rewear.Main
```

## Notes

- Passwords are currently stored in plain text in the `USERS.password_hash` column for demo purposes.
- The app is a desktop Swing client and depends on a running MySQL server.

## Rubric Support

The project includes:

- clear package separation and class structure
- DAO layer for database access
- input validation and custom exceptions
- reusable UI base frame and generic DAO interface
- SQL schema and JDBC integration

## Contact

For any issues, open the project in your editor and inspect the UI frames in `src/main/java/com/rewear/ui/` and DAO logic in `src/main/java/com/rewear/dao/`.
