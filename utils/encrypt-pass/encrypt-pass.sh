#!/bin/bash
jasypt_home="/home/andreas/devtools/jasypt/jasypt-1.9.3"
encryptCmd="$jasypt_home/bin/encrypt.sh"
algorithm="PBEWITHHMACSHA512ANDAES_256"
ivGeneratorClassName="org.jasypt.iv.RandomIvGenerator"

read -s -p "Master password:" masterpass
echo
read -s -p "String to encrypt:" input
echo
"$encryptCmd" input=$input password=$masterpass algorithm=$algorithm ivGeneratorClassName=$ivGeneratorClassName verbose=false
