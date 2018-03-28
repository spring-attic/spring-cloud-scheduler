FROM java:8-alpine

ARG JAR_FILE

ADD target/${JAR_FILE} spring-cloud-scheduler-spi-test-app.jar

ENTRYPOINT ["java", "-jar", "/spring-cloud-scheduler-spi-test-app.jar"]
