# I. Reference
[Single Node GemFire Server by Spring Boot](https://github.com/komushi/spring-boot-gemfire-server)

# II. Quick Start Steps 

## Download and Build

```
git clone https://github.com/komushi/spring-boot-geode-server.git
cd spring-boot-geode-server
mvn clean package -DskipTests
```

## 1. Run with Maven

### which works fine, please play with gfsh.

```
mvn spring-boot:run
```

gfsh and connect

```
gfsh
```

```
gfsh>connect --locator=localhost[10334]
Connecting to Locator at [host=localhost, port=10334] ..
Connecting to Manager at [host=192.168.11.109, port=1099] ..
Successfully connected to: [host=192.168.11.109, port=1099]
```

```
gfsh>query --query="select * from /fact"

Result     : false
startCount : 0
endCount   : 20
Message    : Cannot find regions <[/fact]> in any of the members

NEXT_STEP_NAME : END
```

## 2. Run in java cli

### Work-Round for a gemfire/geode bug.
There is a bug (https://issues.apache.org/jira/browse/GEODE-800). To solve it temporarily please use this patch.
```
./patch-jar.sh
```

Run it with java cli to test with gfsh.

```
java -jar target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar

java -jar target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar --properties.timeout=20
```

## 3. Run with docker deployment

```
mvn clean package -DskipTests

./patch-jar.sh

docker build -t komushi/spring-boot-geode-server:incubating-m2 -f ./Dockerfile .

docker tag komushi/spring-boot-geode-server:incubating-m2 komushi/spring-boot-geode-server:latest

docker run -p 40404:40404 komushi/spring-boot-geode-server
```