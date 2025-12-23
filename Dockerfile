FROM maven:3-eclipse-temurin-25 as build-env

WORKDIR /build

COPY pom.xml /build/pom.xml
RUN mvn -B -f /build/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve-plugins dependency:go-offline

COPY src /build/src
COPY resources /build/resources

RUN JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8 mvn -B -s /usr/share/maven/ref/settings-docker.xml -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build-env /build/target/jwarc-*.jar jwarc.jar
WORKDIR /work

ENTRYPOINT ["java", "-jar", "/app/jwarc.jar"]

