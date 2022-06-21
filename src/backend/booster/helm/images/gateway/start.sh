#!/bin/bash

module="bk-tbs-gateway"

cd $BK_TBS_HOME
chmod +x ${module}

mkdir -p $BK_TBS_LOGS_DIR
chmod 777 $BK_TBS_LOGS_DIR

#ready to start
$BK_TBS_HOME/${module} "$@" --log-dir=$BK_TBS_LOGS_DIR --local-ip=$BK_TBS_LOCAL_IP
