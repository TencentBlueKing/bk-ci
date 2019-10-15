#!/bin/bash

HELP='''
Useage:cmd switch date\n
e.g:switch_index.sh open/close 20180409
'''

if [ $# -lt 2 ];then
    echo -e $HELP
    exit 1
fi

if [ $1 != "open" -a $1 != "close" ];then
    echo "Error:switch mode wrong"
    echo -e $HELP
    exit 1
fi


if [ ${#2} -ne 8 ];then
    echo "Error:Date wrong"
    echo -e $HELP
    exit 1
fi

function warn_report()
{
    warn_str=$1
    warn="/usr/local/agenttools/agent/agentRepStr 1250036 \"$warn_str\""
    echo `date +"[%Y-%m-%d %H:%M:%S]"`WARN_STR:$warn
    warn_result=`eval $warn`
    echo `date +"[%Y-%m-%d %H:%M:%S]"`WARN_RESULT:$warn_result
}

theswitch=$1
thedate=$2
for substr in "00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12" "13" "14" "15" "16" "17" "18" "19" "20" "21" "22" "23"
do
    index_name="log-${thedate:0:4}-${thedate:4:2}-${thedate:6:2}-$substr"
    esmaster_ip="__ES_HOST__"
    curl_cmd="curl -s -XPOST \"http://${esmaster_ip}:__ES_REST_PORT__/${index_name}/_$theswitch\""
    echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_START:$curl_cmd
    curl_result=`eval $curl_cmd`
    echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_END:$curl_result
    resul="SUCCESS"
    if [ `echo $curl_result | grep error |wc -l` != 0 ];then
        resul="FAILED"
        warn_report "$switch index error:$index_name"
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:ERROR:$curl_result
    fi
    if [ `echo $curl_result |grep -Po '(?<=acknowledged":)[a-zA-Z]+'` != "true" ];then
        resul="FAILED"
        warn_report "$switch index error:$index_name"
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:ERROR:$curl_result
    fi
    if [ `echo $curl_result |grep "Unauthorized" |wc -l` != 0 ];then
        resul="FAILED"
        warn_report "$switch index error:$index_name"
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:Unauthorized
    fi
    echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:$resul:$index_name
done

