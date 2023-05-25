#!/bin/bash
unset http_proxy
unset https_proxy

echo "---env---"
env
echo "---------"

allow_raw=$BK_DISTCC_ALLOW
if [[ -z $allow_raw ]]; then
    echo "BK_DISTCC_ALLOW should not be empty"
    exit 101
fi

allow_cmd=""
IFS="," read -ra allow_list <<< "$allow_raw"
for allow in ${allow_list[@]}; do
    allow_cmd=$allow_cmd" --allow "$allow
done

if [[ ! -z $RAND_PORT_SERVICE_PORT ]]; then
    PORT_SERVICE_PORT=$RAND_PORT_SERVICE_PORT
fi

REAL_RAND_KEY=BCS_RANDHOSTPORT_FOR_CONTAINER_PORT_$RAND_PORT_SERVICE_PORT
REAL_RAND_VALUE=$(eval echo \$$REAL_RAND_KEY)
if [[ ! -z $REAL_RAND_VALUE ]]; then
    PORT_SERVICE_PORT=$REAL_RAND_VALUE
fi

port=$PORT_SERVICE_PORT
if [[ -z $port ]]; then
    echo "PORT_SERVICE_PORT should not be empty"
    exit 102
fi

if [[ ! -z $RAND_PORT_STATS_PORT ]]; then
    PORT_STATS_PORT=$RAND_PORT_STATS_PORT
fi

REAL_STATS_RAND_KEY=BCS_RANDHOSTPORT_FOR_CONTAINER_PORT_$RAND_PORT_STATS_PORT
REAL_STATS_RAND_VALUE=$(eval echo \$$REAL_STATS_RAND_KEY)
if [[ ! -z $REAL_STATS_RAND_VALUE ]]; then
    PORT_STATS_PORT=$REAL_STATS_RAND_VALUE
fi

stats_port=$PORT_STATS_PORT
if [[ -z $stats_port ]]; then
    echo "PORT_STATS_PORT should not be empty"
    exit 103
fi

jobs=$BK_DISTCC_JOBS
if [[ -z $jobs ]]; then
    echo "BK_DISTCC_JOBS should not be empty"
    exit 104
fi

adduser -c "Distcc daemons" -s /sbin/nologin distcc
echo > /data/distccd.log
mkdir -p /usr/lib/distcc
chmod 777 /data/distccd.log

distccd \
    --daemon \
    --listen 0.0.0.0 \
    -p $port \
    --stats \
    --stats-port $stats_port \
    --jobs $jobs \
    --user distcc \
    --log-file /data/distccd.log \
    --log-level debug \
    $allow_cmd &

sleep 10s

while true; do
    distccd_num=$(ps -ef | grep distccd | wc -l)
    if [[ $distccd_num == "1" ]]; then
        echo "There is no distccd process running"
            exit 201
    fi
    sleep 1s
done