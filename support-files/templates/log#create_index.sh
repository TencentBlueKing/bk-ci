#!/bin/bash

HELP='''
Useage:cmd date\n
e.g:create_index.sh 20180409
'''

if [ $# == 0 ];then
    echo -e $HELP
    exit 1
fi

if [ ${#1} -ne 8 ];then
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


thedate=$1
for substr in "00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12" "13" "14" "15" "16" "17" "18" "19" "20" "21" "22" "23"
do
    index_name="log-${thedate:0:4}-${thedate:4:2}-${thedate:6:2}-$substr"
    esmaster_ip="__ES_HOST__"
    echo `date +"[%Y-%m-%d %H:%M:%S]"`"CURL_START:create index:${index_name}"
    curl_result=`curl -s -XPUT "http://${esmaster_ip}:__ES_REST_PORT__/${index_name}?" -H 'Content-Type: application/json' -d'
    {
        "settings" : {
            "index" : {
                "number_of_shards" : 3, 
                "number_of_replicas" : 1, 
                "refresh_interval" : "3s", 
                "queries" : {
                    "cache" : {
                        "enabled" : false 
                    }
                }
            }
        },
        "mapping" : {
            "typeForResetting" : {
                "properties" : {
                    "lineNo" : {"type" : "long"},
                    "message" : {"type" :"text","analyzer" : "standard","fielddata" : false},
                    "timestamp" : {"type" : "long"}
                }
            }
        }
    }
    '
    `

    #echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_END:$curl_result

    resul="SUCCESS"
    if [ `echo $curl_result |grep "index_already_exists_exception" |wc -l` != 0 ];then
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:EXISTS:$curl_result
    fi


    if [ `echo $curl_result | grep error | grep -v "index_already_exists_exception" |wc -l` != 0 ];then
        resul="FAILED"
        warn_report "Create index error:$index_name"
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:ERROR:$curl_result
    fi

    if [ `echo $curl_result |grep "Unauthorized" |wc -l` != 0 ];then
        resul="FAILED"
        warn_report "Create index error:$index_name"
        echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:Unauthorized
    fi


    echo `date +"[%Y-%m-%d %H:%M:%S]"`CURL_RESULT:$resul:$index_name
done
