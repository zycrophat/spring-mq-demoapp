<#
MIT License

Copyright (c) 2020 Andreas Steffan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
#>

Param(
    [string] $Entropy
)
function SecureStringToString([System.Security.SecureString] $secureString) {
    $([System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureString)))
}
[void] [Reflection.Assembly]::LoadWithPartialName("System.Security")
$scope = [System.Security.Cryptography.DataProtectionScope]::CurrentUser
$encoding = New-Object System.Text.UTF8Encoding $False
$entropyBytes = $null
if ($Entropy -ne "")
{
    $entropyBytes = [System.Convert]::FromBase64String($Entropy)
}

$plainText = Read-Host "String to encrypt" -AsSecureString
$plainTextBytes = $encoding.GetBytes($(SecureStringToString($plainText)))

$ciphertext = [System.Security.Cryptography.ProtectedData]::Protect(
        $plainTextBytes, $entropyBytes, $scope)
[System.Convert]::ToBase64String($ciphertext)
