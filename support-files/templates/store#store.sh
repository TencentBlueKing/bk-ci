#!/bin/bash
#-------------------------------------------------------------------------------------------------------------
#该脚本的使用方式为-->[sh xxx.sh start|stop|status|restart]
#该脚本可在服务器上的任意目录下执行,不会影响到日志的输出位置等
#-------------------------------------------------------------------------------------------------------------

# 公共参数，不要修改
PROJECT_NAME=bkci

# 服务相关修改
SERVICE_NAME=store
# HTTP端口的是占位符号 需要根据application-store.yml中设置的端口来替换
HTTP_PORT=__BKCI_STORE_API_PORT__

# 详细的日志console.log开关，放开注释即可打开
NOHUPLOG=/dev/null
#NOHUPLOG=${PROJECT_NAME}_${SERVICE_NAME}_console.log

#-------------------------------------------------------------------------------------------------------------
#       系统运行参数   运行环境: JDK_1.8+
#-------------------------------------------------------------------------------------------------------------
SERVICE_HOME=$(cd "$(dirname "$0")"; pwd)
if [[ ! -n "${BK_HOME}" ]]; then
    BK_HOME=$(cd "$(dirname "$0")"/../..; pwd)
fi

#BK_HOME=$(cd ${BASH_SOURCE%/*}; echo ${PWD%%/${PROJECT_NAME}*})
#if [[ -f ${BK_HOME}/bksuite-${PROJECT_NAME}.env ]]; then
#    source ${BK_HOME}/bksuite-${PROJECT_NAME}.env
#fi

CONF_HOME=${BK_HOME}/etc/${PROJECT_NAME}

SHELL_FILE_NAME=${0##*/}
APP_PRIVATE_CONF_DIR=${SERVICE_HOME}/config
JAR_FILE=${SERVICE_HOME}/boot-${SERVICE_NAME}.jar
LOGS_HOME=${BK_HOME}/logs/${PROJECT_NAME}/${SERVICE_NAME}/

if [[ -z "${CERT_PATH}" ]]; then
    CERT_PATH=${BK_HOME}/cert
fi


if [[ ! -n "${JAVA_HOME}" ]]||[[ ! -d "${JAVA_HOME}" ]]; then
    JAVA_HOME="${BK_HOME}/service/java"
    if [[ ! -d "${JAVA_HOME}" ]]; then
        JAVA_HOME="${BK_HOME}/common/java"
    fi
    export JAVA_HOME
    export PATH=${JAVA_HOME}/bin:$PATH
fi

export CLASSPATH=${APP_PRIVATE_CONF_DIR}:.


function getPID(){
    javaps=`${JAVA_HOME}/bin/jps -l | grep "${JAR_FILE}"`
    if [[ -n "$javaps" ]]; then
        PID=`echo ${javaps} | awk '{print $1}'`
    else
        PID=0
    fi
}

HTTP_PORT_STATUS=0

# 启动job服务器
function startup() {
    WEB_BASEDIR=${SERVICE_HOME}/rundata
    #	说明：以下最小内存和最大内存 当配置时最好配置成一样的,32GB机器上配置16GB内存，64GB机器配置32GB内存
    #	export JOB_JAVA_OPTS="-Xms8g -Xmx8g"
#    JAVA_OPTS="-Dserver.address=__LAN_IP__"
#    JAVA_OPTS="$JAVA_OPTS -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:MaxGCPauseMillis=800"
#    JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:$LOGS_HOME/gc_${SERVICE_NAME}.log "
    JAVA_OPTS="$JAVA_OPTS -server -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
    JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=file:${CONF_HOME}/common.yml,file:${CONF_HOME}/application-${SERVICE_NAME}.yml"
    JAVA_OPTS="$JAVA_OPTS -Dservice.log.dir=${LOGS_HOME}"

    export JAVA_OPTS

    getPID
    if [[ ${PID} -ne 0 ]]; then
        echo "${PROJECT_NAME}-${SERVICE_NAME} already started(PID=$PID)"
    else
        echo -n "Starting ${PROJECT_NAME}-${SERVICE_NAME}"
        if [[ ! -d "${LOGS_HOME}" ]]; then
            mkdir -p "${LOGS_HOME}"
        fi

        checkPortStatus
        if [[ ${HTTP_PORT_STATUS} -ne 0 ]]; then
            echo "[Failed]-- http port ${HTTP_PORT} has been used!"
            exit 1
        fi

        nohup ${JAVA_HOME}/bin/java ${JAVA_OPTS} -classpath ${CLASSPATH} -jar ${JAR_FILE} > ${NOHUPLOG} 2>&1 &

        for i in $(seq 100)
        do
        sleep 0.5
        echo -e ".\c"
        getPID
        if [[ ${PID} -ne 0 ]]; then
            checkPortStatus " ${PID} "
            if [[ ${HTTP_PORT_STATUS} -gt 0 ]]; then
                break;
            fi
        fi
        done

        getPID
        if [[ ${PID} -eq 0 ]]; then
            echo "[Failed]"
            exit 1
        fi

        checkPortStatus " ${PID} "
        if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
            echo "[Failed]-- http port ${HTTP_PORT} start fail!"
            exit 1
        fi

        echo "(PID=$PID)...[Success]"

    fi
}

function checkPortStatus() {
    # $1 is pid
    if [[ -n "$1" ]]; then
        dataTmp1=`lsof -nP  -iTCP:${HTTP_PORT} -sTCP:LISTEN  | grep "$1" -c`
    else
        dataTmp1=`lsof -nP  -iTCP:${HTTP_PORT} -sTCP:LISTEN | wc -l`
    fi

    if [[ -n "${dataTmp1}" ]]; then
        HTTP_PORT_STATUS=`echo ${dataTmp1} | awk '{print $1}'`
    else
        HTTP_PORT_STATUS=0
    fi
}

# 停止job服务器
function shutdown(){
    getPID
    if [[ ${PID} -ne 0 ]]; then
        echo -n "Stopping ${PROJECT_NAME}-${SERVICE_NAME}(PID=${PID})..."
        kill ${PID}
        if [[ $? -ne 0 ]]; then
            echo "[Failed]"
            exit 1
        fi
        for i in $(seq 20)
        do
            sleep 0.5
            getPID
            if [[ ${PID} -eq 0 ]]; then

                checkPortStatus " ${PID} "

                if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
                    break
                fi
            fi
            echo -e ".\c"
        done
        getPID
        if [[ ${PID} -eq 0 ]]; then
            echo "[Success]"
        else
            kill -9 ${PID}
            if [[ $? -ne 0 ]]; then
                echo "[Failed]"
                exit 1
            fi
            echo "some task is running in background,force stop it.[Success]"
        fi
    else
        echo "${PROJECT_NAME}-${SERVICE_NAME} is not running"
    fi
}

function getServerStatus(){
    getPID
    if [[ ${PID} -ne 0 ]]; then
        checkPortStatus " ${PID} "
        if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
            echo "${PROJECT_NAME}-${SERVICE_NAME} port ${HTTP_PORT} is not listening(PID=${PID})";
            exit 99;
        fi
        echo "${PROJECT_NAME}-${SERVICE_NAME} is running(PID=${PID})"
    else
        echo "${PROJECT_NAME}-${SERVICE_NAME} is not running"
    fi
}

# 重启JOB
function restart(){
    shutdown
    sleep 1
    startup
}


case "$1" in
restart)
    restart
    ;;
start)
    startup
    ;;
stop)
    shutdown
    ;;
status)
    getServerStatus
    ;;
*)
 echo $"Usage: ${SHELL_FILE_NAME} {start|stop|status|restart}"
esac