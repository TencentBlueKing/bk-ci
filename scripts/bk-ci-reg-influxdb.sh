#!/bin/bash
SELF_DIR=$(dirname "$(readlink -f "$0")")
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

source ${CTRL_DIR:-/data/install}/load_env.sh
source ${CTRL_DIR:-/data/install}/bin/04-final/ci.env
export INFLUXDB_PORT=8086
INFLUXDB_VERSION=1.7.10

if [[ ${LAN_IP} != "${BK_CI_IP0}" ]] ; then echo "仅支持在CI第一台服务运行" ; exit 1 ; fi
if grep -w "disable rsyslog rate limit" /etc/rsyslog.conf ; then sed -i -e '/disable rsyslog rate limit/s/^/#&/' /etc/rsyslog.conf ; fi
if grep -w "disable journald rate limit" /etc/rsyslog.conf ; then sed -i -e '/disable journald rate limit/s/^/#&/' /etc/rsyslog.conf ; fi

port_test () {
ip="$1"
port=$2
resFile=/tmp/influxdb_test_temptelnnfdswg

#echo "telnet $ip $port..."
/usr/bin/expect >${resFile} 2>&1 <<EOF
set timeout 5
spawn telnet  $ip $port
expect {
"Connected*" {
     send "q\r"
     exit 0
    }
"Connection refused" {
     puts "refused\r"
     exit 1
    }
timeout {
     puts "timeout\r"
     exit 2
    }
}
exit
expect eof
EOF
}

if rpm -qa|grep -w influxdb-${INFLUXDB_VERSION} ; then
    echo "influxdb already installed"
else
    echo "install influxdb now"
    if ${CTRL_DIR}/bin/install_influxdb.sh -b 0.0.0.0 -P ${INFLUXDB_PORT} -d ${INSTALL_PATH}/public/influxdb -l ${INSTALL_PATH}/logs/influxdb -w ${INSTALL_PATH}/public/influxdb/wal -p ${BK_INFLUXDB_ADMIN_PASSWORD} -u admin ; then
        echo "influxdb install success"
    else
        echo "influxdb install failed" ; exit 1
   fi
fi

#port_test $BK_INFLUXDB_IP0 $INFLUXDB_PORT
#
#if [[ ! -z `grep -w "Escape" /tmp/influxdb_test_temptelnnfdswg` ]] ; then
#    echo "bkmonitorv3 influxdb exist"
#    if influx -host $BK_INFLUXDB_IP0 -port $INFLUXDB_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute "SHOW DATABASES;"|grep agentMetrix ; then
#        echo "ci influxdb database agentMetrix already created on bkmonitorv3 influxdb"
#    else
#        if influx -host $BK_INFLUXDB_IP0 -port $INFLUXDB_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute "CREATE DATABASE agentMetrix" ; then
#            echo "ci influxdb database agentMetrix create success on bkmonitorv3 influxdb"
#        else
#            echo "ci influxdb database agentMetrix create failed on bkmonitorv3 influxdb" ; exit 2
#        fi
#    fi
#else
#    echo "bkmonitorv3 influxdb not exist , create influxdb on ci-01"
    systemctl start influxd.service ; sleep 5
    if systemctl status influxd.service|grep -w "active (running)" ; then
        echo "influxdb start success installed"
        if influx -host $BK_CI_IP0 -port $INFLUXDB_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute "SHOW DATABASES;"|grep agentMetrix ; then
            echo "influxdb agentMetrix already created on ci-01 influxdb"
        else
            if influx -host $BK_CI_IP0 -port $INFLUXDB_PORT -username $BK_INFLUXDB_ADMIN_USER -password $BK_INFLUXDB_ADMIN_PASSWORD -execute "CREATE DATABASE agentMetrix" ; then
                echo "influxdb agentMetrix create success on ci-01 influxdb"
            else
               echo "influxdb agentMetrix create failed on ci-01 influxdb" ; exit 3
            fi
        fi
    else
            echo "influxdb start failed" ; exit 5
    fi
#fi

#rm -f /tmp/influxdb_test_temptelnnfdswg 
