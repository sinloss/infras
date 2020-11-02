@echo off
setlocal

where openssl >nul 2>&1 || goto openssl_not_found
if not exist "%~dp0target" mkdir "%~dp0target"
pushd "%~dp0target"

REM Generate a 2048-bit RSA private key
openssl genrsa -out pk.pem 2048

REM Convert to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform DER -in pk.pem -out key -nocrypt

REM Output public key
openssl rsa -in pk.pem -pubout -outform DER -out key.pub

popd
exit /b

:openssl_not_found
echo Could not find the openssl executable in PATH
exit /b 1