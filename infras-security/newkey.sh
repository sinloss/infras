#!/bin/bash

type openssl || { echo "Could not find the openssl executable in PATH"; exit 1; }

wd="$(cd `dirname $0`; pwd)"
[[ -e "$wd/target" ]] || mkdir "$wd/target"
pushd "$wd/target"

# Generate a 2048-bit RSA private key
openssl genrsa -out pk.pem 2048

# Convert to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform DER -in pk.pem -out key -nocrypt

# Output public key
openssl rsa -in pk.pem -pubout -outform DER -out key.pub

pod