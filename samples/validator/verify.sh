#!/usr/bin/env bash
source ${PWD%/*samples/*}/scripts/wait.sh

if [ -f ./target/validator ]; then
  EXECUTABLE="./target/validator -Dapp.value=123"
else
  EXECUTABLE="java -DspringAot=true -Dapp.value=123 -jar target/*.jar"
fi
RESPONSE=`$EXECUTABLE 2>&1 | grep "Reason: Invalid lastname"`
if [[ "$RESPONSE" != *"Reason: Invalid lastname"* ]]; then
  echo "Response for app.value=123 is invalid: $RESPONSE"
  exit 1
fi
trap 'wait_command_output "curl -d \"testIntMin=-1\" -s localhost:8080/validateForm" "Validation failed: -1"' ERR
wait_command_output "curl -d \"testIntMin=1\" -s localhost:8080/validateForm" "Validation passed: 1"
