## Running tests
`./gradlew clean test`

## Running the app
```
./gradlew clean build
java -jar ./build/libs/transaction-1.0-all.jar
```

The app runs at port number **4567**

## Available APIs

|Title |HTTP Method| URL|
|-----|------|-----|
|Get list of accounts with available balance | GET | /accounts|
|Get account with available balance for the accountId | GET | /accounts/:accountId|
|Transfer money from one account to another | POST | /transaction|

## Seed data
The application loads some accounts and users into in-memory H2 database on app-start.
The seed data sql script is available at `src/main/resources/db.seed/accounts.sql`

## Tech stack
**Language**: Kotlin

**Database**: H2 In-memory 

### Libraries used

|Name | Use|
|-----|------|
|SparkJava |HTTP micro framework|
|Javamoney |For high precision monetary calculation|
|Google Guice |Dependency injection|
|Flyway |SQL database migration|
|Jooq |SQL database communication|
|HikariCP |Database connection pool|
|Gson |JSON to Kotlin serialization deserialization|
|Slf4j & Logback |Logging|
|JUnit5 |For unit testing|
|Rest-assured |REST API testing|
|Mockk |For Kotlin mocking|
 
