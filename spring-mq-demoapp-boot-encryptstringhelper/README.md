# spring-mq-demoapp-boot-encryptstringhelper

Commandline utility to encrypt strings using the [Jasypt](http://www.jasypt.org/) library.

## Prerequisites

- JDK 11

## How to build

``` bash
$ ./gradlew installDist
```

## How to run

``` bash
$ cd build/install/spring-mq-demoapp-boot-encryptstringhelper/bin
$ ./spring-mq-demoapp-boot-encryptstringhelper
```
The program then prompts for the encryption password and the value to be
encrypted.
The cipher text will then be written to stdout.

The encryption password can also be provided using the `ENC_PASSWORD`
environment variable or the `-DencPassword` JVM system property
(see next section).

## Configuration

By default the `PBEWITHHMACSHA512ANDAES_256` encryption algorithm and
[`org.jasypt.iv.RandomIvGenerator`](http://www.jasypt.org/api/jasypt/1.9.3/org/jasypt/iv/RandomIvGenerator.html)
IV-generator are used.

Configuration is possible using environment variables or JVM system
properties.

Example:
``` bash
$ export JAVA_OPTS="-DencStringOutputType=hexadecimal" # JVM system properties must be prefixed with enc and must be in camelCase format
$ export ENC_PASSWORD=secret # environment variables must be prefixed with ENC_ and must be in underscore separated format
$ export ENC_SALT_GENERATOR_CLASS_NAME=org.jasypt.salt.RandomSaltGenerator
$ ./spring-mq-demoapp-boot-encryptstringhelper
```

Configuration is possible for the properties of jasypt's
[`EnvironmentStringPBEConfig`](http://www.jasypt.org/api/jasypt/1.9.3/org/jasypt/encryption/pbe/config/EnvironmentStringPBEConfig.html)
class.

If a JVM system property and an environment variable are set for the
same configuration option, the JVM system property takes precedence.
