#!/bin/bash
set -e
devopsCallbackUrl=$1
P4PORT=$2
event_type=$3
CHANGE=$4
TRIGGER_TYPE=CHANGE

echo "Running devops change.sh\n\n"
echo "devopsCallbackUrl:$devopsCallbackUrl,P4PORT:$P4PORT,event_type:$event_type,CHANGE:$CHANGE"

curl --header 'Content-Type: application/json' \
     --connect-timeout 3 \
     --request POST \
     --silent \
     --data "{\"change\":$CHANGE,
     \"p4Port\":\"$P4PORT\",
     \"trigger_type\":\"$TRIGGER_TYPE\",
     \"event_type\":\"$event_type\"
     }" \
     "$devopsCallbackUrl" || { echo "Sending webhook failed"; exit 0; }
echo "Sending webhook finished\n\n"
exit 0
