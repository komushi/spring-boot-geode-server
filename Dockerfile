FROM anapsix/alpine-java:8
COPY target/spring-boot-geode-server-0.0.1-SNAPSHOT.jar /maven/
EXPOSE 8080 10334 40404 1099 7070
VOLUME ["/tmp"]
ENTRYPOINT ["java","-jar","/maven/spring-boot-geode-server-0.0.1-SNAPSHOT.jar"]
