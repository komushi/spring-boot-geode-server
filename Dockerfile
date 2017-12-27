FROM anapsix/alpine-java:8
COPY target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar /maven/spring-boot-geode-server.jar
EXPOSE 40404
VOLUME ["/tmp"]
ENTRYPOINT ["java","-jar","/maven/spring-boot-geode-server.jar","--properties.useLocator=false","--properties.useJmx=false"]
