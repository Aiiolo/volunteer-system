FROM docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mkdir -p /root/.m2 && echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml
COPY src ./src
RUN mvn -DskipTests package

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /workspace/target/volunteer-system-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]