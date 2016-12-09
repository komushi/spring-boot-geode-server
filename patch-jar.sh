#!/bin/bash
tar -zxvf spring-shell-classes.tar.gz
tar -zxvf geode-classes.tar.gz
jar -uvf target/spring-boot-geode-server-0.0.1-SNAPSHOT.jar org/apache/geode/management/internal/cli/commands/
jar -uvf target/spring-boot-geode-server-0.0.1-SNAPSHOT.jar org/apache/geode/management/internal/cli/converters/
jar -uvf target/spring-boot-geode-server-0.0.1-SNAPSHOT.jar org/springframework/shell/converters/
rm -rf org/