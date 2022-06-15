#!/bin/bash

source /root/.profile
/docker-entrypoint.sh "nginx" "-g" "daemon off;"