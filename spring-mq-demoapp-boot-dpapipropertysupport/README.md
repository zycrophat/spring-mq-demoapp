# spring-mq-demoapp-boot-dpapipropertysupport

Library to support Windows DataProtection API (DPAPI) encrypted  
configuration properties in Spring Boot config files, e.g. for encrypted  
credentials.

## Prerequisites

- JDK 11
- Microsoft Windows

## How to use

Add this module's JAR to the Spring application's runtime classpath.

## How to encrypt

In order to encrypt a property value the PowerShell scripts in the
`./dpapipasswordhelper` directory can be used:

1. (Optional) Generate random "Entropy" that is used by the DPAPI key
   derivation mechanism in addition to user account data:
   ``` powershell
   PS .\dpapipasswordhelper\generateEntropy.ps1
   PgrJmrefm9GsBNzJJXH8lUe2sX5tnpJUYX9MjhAkagM=
   ```
   This will generate 256 bit of random entropy in Base64 encoded form.

2. Encrypt the desired text:
    ``` powershell
   PS .\dpapipasswordhelper\encryptText.ps1 -Entropy PgrJmrefm9GsBNzJJXH8lUe2sX5tnpJUYX9MjhAkagM=
   String to encrypt: ***********
   AQAAAN.....iNpZ137Ka2SQ=
   ```
   The output the cipher text in in Base64 encoded form.  
   If you skipped step 1, omit the `Entropy` argument.

3. Add the encrypted value in `DPAPI(<ciphertext>)` form to a Spring
   config file.
   For example:
   ``` yaml
   my:
    secret: DPAPI(AQAAAN.....iNpZ137Ka2SQ=)
   ```

## How to build

``` bash
$ ./gradlew jar
```

## See also
- [Windows Data Protection](https://docs.microsoft.com/en-us/previous-versions/ms995355(v%3Dmsdn.10))
- [windpapi4j](https://github.com/peter-gergely-horvath/windpapi4j)
