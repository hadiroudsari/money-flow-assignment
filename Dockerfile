FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q verify && cp target/*.jar app.jar

RUN mkdir extracted && cd extracted && jar xf ../app.jar
RUN jdeps --ignore-missing-deps -q --recursive --multi-release 25 \
          --print-module-deps \
          --class-path 'extracted/BOOT-INF/lib/*' \
          extracted/BOOT-INF/classes > modules.txt

RUN jlink --add-modules $(cat modules.txt),jdk.unsupported,jdk.management \
          --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
          --output /jre

FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=build /jre /opt/jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
