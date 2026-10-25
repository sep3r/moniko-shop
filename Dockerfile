# Dockerfile ریشه‌ی ریپو: فقط بک‌اند (پوشه‌ی backend) را build می‌کند.
# برای استقرار از گیت‌هاب روی رانفلر بدون نیاز به تنظیم زیرپوشه.
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
