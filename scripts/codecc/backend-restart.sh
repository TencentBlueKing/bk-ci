#!/bin/bash

backends=(task defect report asyncreport codeccjob schedule openapi apiquery quartz)
for service in ${backends[@]};
do
  echo "stop & start the service ${service}"
        systemctl stop ${service}
  systemctl start ${service}
done