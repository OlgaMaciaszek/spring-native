#!/usr/bin/env bash
source ${PWD%/*samples/*}/scripts/wait.sh
trap 'wait_command_output "rsc --debug --request --data \"{\"origin\":\"Client\",\"interaction\":\"Request\"}\" --route request-response tcp://localhost:7000" "Frame => Stream ID: 1 Type: NEXT_COMPLETE Flags: 0b1100000 Length: 81"' ERR
wait_command_output 'rsc --debug --request --data "{\"origin\":\"Client\",\"interaction\":\"Request\"}" --route mono-request-response tcp://localhost:7000' "Frame => Stream ID: 1 Type: NEXT_COMPLETE Flags: 0b1100000 Length: 81"
