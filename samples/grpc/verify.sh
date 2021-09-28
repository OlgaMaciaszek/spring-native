#!/usr/bin/env bash
source ${PWD%/*samples/*}/scripts/wait.sh
trap 'wait_command_output "grpcurl -plaintext localhost:50051 describe demo.Greeter 2>&1" "demo.Greeter is a service:"' ERR
wait_command_output "grpcurl -plaintext -d '{}' localhost:50051 demo.Greeter/Hello 2>&1" "\"firstName\": \"Josh\","
