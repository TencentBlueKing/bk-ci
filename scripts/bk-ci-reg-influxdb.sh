#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

source ${CTRL_DIR:-/data/install}/load_env.sh
source ${CTRL_DIR:-/data/install}/bin/04-final/ci.env
export MYSQL_PWD=$BK_PAAS_MYSQL_PASSWORD

[ -z "$app_token" ] && { echo "无法获取app_token" ; exit 1; }

if ping -c 3 $BK_INFLUXDB_PROXY_HOST|grep "3 received" ; then
    if influx -host $BK_INFLUXDB_PROXY_HOST -port $BK_INFLUXDB_PROXY_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute "show databases;"|grep agentMetrix
        echo "influxdb agentMetrix already created"
    else
        ssh $BK_INFLUXDB_IP "source /data/install/utils.fc ; influx -host $BK_INFLUXDB_IP -port $BK_CI_INFLUXDB_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute \"show databases;\""|grep agentMetrix
    fi
else


fi

