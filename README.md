[![GitHub](https://img.shields.io/github/license/zycrophat/spring-mq-demoapp)](https://raw.githubusercontent.com/zycrophat/spring-mq-demoapp/master/LICENSE)
[![Build Status](https://dev.azure.com/zycrophat/spring-mq-demoapp/_apis/build/status/zycrophat.spring-mq-demoapp?branchName=master)](https://dev.azure.com/zycrophat/spring-mq-demoapp/_build/latest?definitionId=1&branchName=master)
[![Build Status](https://vsrm.dev.azure.com/zycrophat/_apis/public/Release/badge/12ae4833-2d84-414d-b37a-111fa1dffb77/1/1)](https://dev.azure.com/zycrophat/spring-mq-demoapp/_release?definitionId=1)
# spring-mq-demoapp

Demo playground application for Kotlin, Spring, Camel and other things.

## Prerequisites

- JDK 11
- Docker and Docker Compose (for providing an ActiveMQ broker)

## How to build

``` bash
$ ./gradlew installBootDist
```

## How to run

``` bash
$ ./gradlew bootRun
```

## How to build a Windows Service

You can build a [winsw](https://github.com/kohsuke/winsw) based Windows service using the
following commands:
``` bash
$ ./gradlew installBootWinServiceDist
```

To install & uninstall the service admin privileges are required:
``` bash
$ cd spring-mq-demoapp-boot-sampleservice/build/install/spring-mq-demoapp-boot-sampleservice-bootWinService
$ ./spring-mq-demoapp-boot-sampleservice-x.y.z.exe install
$ ./spring-mq-demoapp-boot-sampleservice-x.y.z.exe uninstall
```

This also works for the spring-mq-demoapp-boot-admin subproject.

Distribution zip files can also be created:
``` bash
$ ./gradlew bootWinServiceDistZip
```
