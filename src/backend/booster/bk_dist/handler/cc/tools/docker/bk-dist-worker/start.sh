#! /bin/bash

echo "---env---"
env
echo "---------"

cd /data/bcss/bk-dist-worker

# make log-dir if not exists.
if [[ ! -d "./logs" ]];then
    mkdir ./logs
fi

# make module executable.
chmod +x bk-dist-worker

if [[ ! -z $RAND_PORT_SERVICE_PORT ]]; then
   PORT_SERVICE_PORT=$RAND_PORT_SERVICE_PORT
fi

port=$PORT_SERVICE_PORT
if [[ -z $port ]]; then
   echo "PORT_SERVICE_PORT should not be empty"
   exit 102
else
   echo "PORT_SERVICE_PORT is ${port}"
   export BK_DIST_PORT_4_WORKER=${port}
fi

echo "BK_DIST_MAX_PROCESS_4_WORKER=${BK_DIST_MAX_PROCESS_4_WORKER}"
echo "BK_DIST_WHITE_IP=${BK_DIST_WHITE_IP}"

source ~/.bashrc
./bk-dist-worker -f config_file.json > ./logs/bk-dist-worker.log 2>&1 &

sleep 10s

while true; do
   worker_num=$(ps -ef | grep -F "bk-dist-worker" | grep -v grep | wc -l)
   if [[ $worker_num == "1" ]]; then
      echo "There is no worker process running"
      exit 201
   fi
   sleep 1s
done