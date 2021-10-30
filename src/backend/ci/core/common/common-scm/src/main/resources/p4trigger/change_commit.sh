#!/bin/bash
set -e
devopsCallbackUrl=$1
P4PORT=$2
CHANGE=$3
TRIGGER_TYPE=CHANGE_COMMIT

echo "Running devops commit_change.sh\n\n"

curl --header 'Content-Type: application/json' \
     --connect-timeout 3 \
     --request POST \
     --silent \
     --data "{\"change\":$CHANGE,
     \"p4Port\":\"$P4PORT\",
     \"trigger_type\":\"$TRIGGER_TYPE\"
     }" \
     $devopsCallbackUrl
echo "Sending webhook finished\n\n"
exit 0
