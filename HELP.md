# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.6/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.6/gradle-plugin/reference/html/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.6/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Building and Running the Project

#### Prerequisites
- Java 17 or higher
- Gradle 8.5 (included via wrapper)

#### Build the project
```bash
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

#### Run the application
```bash
# Windows
.\gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

#### Run tests
```bash
# Windows
.\gradlew.bat test

# Linux/Mac
./gradlew test
```

#### Create executable JAR
```bash
# Windows
.\gradlew.bat bootJar

# Linux/Mac
./gradlew bootJar
```

The JAR file will be created in `build/libs/` directory.

