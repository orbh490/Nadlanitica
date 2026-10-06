# בנייה מרובת-שלבים: שלב ה-build כולל את כל Maven+JDK, שלב ההרצה הסופי נשאר קטן (JRE בלבד, בלי קוד המקור).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/analytics-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
