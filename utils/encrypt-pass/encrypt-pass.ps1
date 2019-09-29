$jasypt_home="C:\devtools\jasypt-1.9.3"
$encryptCmd="$jasypt_home\bin\encrypt.bat"
$algorithm="PBEWITHHMACSHA512ANDAES_256"
$ivGeneratorClassName="org.jasypt.iv.RandomIvGenerator"

function SecureStringToString([System.Security.SecureString] $secureString) {
    $([System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureString)))
}

$masterpass = Read-Host "Master password" -AsSecureString
$input = Read-Host "String to encrypt" -AsSecureString
& "$encryptCmd" "input=$(SecureStringToString($input))", "password=$(SecureStringToString($masterpass))", "algorithm=$algorithm", "ivGeneratorClassName=$ivGeneratorClassName", "verbose=false"
