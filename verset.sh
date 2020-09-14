#!/bin/bash

type mvn || { echo "Could not find the mvn script in PATH"; exit 1; }

if [[ -z "$1" ]]; then
  mvn versions:set
else
  mvn versions:set versions:update-child-modules -DnewVersion=$1
fi