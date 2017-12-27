#!/bin/bash
tar -zxvf spring-shell-classes.tar.gz
tar -zxvf gemfire-classes.tar.gz
jar -uvf target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar com/gemstone/gemfire/management/internal/cli/commands/
jar -uvf target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar com/gemstone/gemfire/management/internal/cli/converters/
jar -uvf target/spring-boot-geode-server-incubating-m2-1.0.0-RELEASE.jar org/springframework/shell/converters/
rm -rf com/
rm -rf org/