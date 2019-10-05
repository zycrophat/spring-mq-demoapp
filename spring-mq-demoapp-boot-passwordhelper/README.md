# spring-mq-demoapp-boot-passwordhelper

Commandline utility to create password hashes using Spring Security's
[`DelegatingPasswordEncoder`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/password/DelegatingPasswordEncoder.html).

## Prerequisites

- JDK 11

## How to build

``` bash
$ ./gradlew installDist
```

## How to run

``` bash
$ cd build/install/spring-mq-demoapp-boot-passwordhelper/bin
$ ./spring-mq-demoapp-boot-passwordhelper
```
The program then prompts for the password to be hashed.
The password hash will then be printed to stdout in the
[`DelegatingPasswordEncoder`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/password/DelegatingPasswordEncoder.html)
class's storage format (e.g., `{bcrypt}$2a$10$vrPX9Th7Y3N7Qftm5SIzVOp/omtgHcpKnidMtUVtXpJaonrGSw5lm`).
