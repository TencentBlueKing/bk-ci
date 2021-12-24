#!/bin/bash
set -e
devopsCallbackUrl=$1
p4Port=$2
event_type=$3
change=$4
trigger_type=$5
user=$6

echo "Running devops change.sh\n\n"
echo "devopsCallbackUrl:$devopsCallbackUrl,p4Port:$p4Port,event_type:$event_type,CHANGE:$change,user:$user"

curl --header 'Content-Type: application/json' \
     --connect-timeout 3 \
     --request POST \
     --silent \
     --data "{\"change\":$change,
     \"p4Port\":\"$p4Port\",
     \"trigger_type\":\"$trigger_type\",
     \"event_type\":\"$event_type\",
	   \"user\":\"$user\"
     }" \
     "$devopsCallbackUrl" || { echo "Sending webhook failed"; exit 0; }
echo "Sending webhook finished\n\n"
exit 0
