<template>
    <div class="pull-code-panel bk-form bk-form-vertical">
        <section>
            <form-field v-if="!obj.hidden" v-for="(obj, key) of newModel" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                <component :is="obj.component" :name="key" :disabled="disabled" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="(key === 'envType') ? handleChooseEnvType : (key === 'type') ? handleChooseType : handleUpdateElement" :value="element[key]" v-bind="obj"></component>
            </form-field>
        </section>
    </div>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { Base64 } from 'js-base64'

    export default {
        name: 'jobDevopsFastExecuteScript',
        mixins: [atomMixin, validMixins],
        data () {
            return {
                newModel: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        async mounted () {
            this.newModel = this.atomPropsModel
            if (this.element.envType) {
                this.handleChooseEnvType('envType', this.element.envType)
            }
            if (this.element.type) {
                this.newModel.type.value = this.element.type.toString()
                this.handleChooseType('type', this.element.type.toString())
            }
            if (this.element.content) {
                this.newModel.showContent.value = Base64.decode(this.element.content)
                this.handleUpdateElement('showContent', Base64.decode(this.element.content))
            }
            if (this.element.scriptParams) {
                this.newModel.showScriptParams.value = Base64.decode(this.element.scriptParams)
                this.handleUpdateElement('showScriptParams', Base64.decode(this.element.scriptParams))
            }
            if (this.element.envType === 'NODE') {
                // this.element.nodeId.push('aaa')
                await this.filterNotExistData('nodeId')
            }
            // 把envName转换成字符串
            const envName = this.element.envName ? this.element.envName.join(',') : ''
            this.handleUpdateElement('envName', envName)
        },
        destroyed () {
            if (this.element.showContent !== undefined) {
                this.handleUpdateElement('content', Base64.encode(this.element.showContent))
            }
            if (this.element.showScriptParams !== undefined) {
                this.handleUpdateElement('scriptParams', Base64.encode(this.element.showScriptParams))
            }

            let arr = []
            if (this.element.envName) {
                arr = this.element.envName.split(',')
            }
            this.handleUpdateElement('envName', arr)
        },
        methods: {
            handleChooseEnvType (name, value) {
                if (value === 'ENV') {
                    this.newModel.envId.hidden = false
                    this.newModel.envName.hidden = true
                    this.newModel.nodeId.hidden = true
                } else if (value === 'ENV_NAME') {
                    this.newModel.envId.hidden = true
                    this.newModel.envName.hidden = false
                    this.newModel.nodeId.hidden = true
                } else {
                    this.newModel.envId.hidden = true
                    this.newModel.envName.hidden = true
                    this.newModel.nodeId.hidden = false
                }
                this.handleUpdateElement(name, value)
            },
            handleChooseType (name, value) {
                let con = ''
                switch (value) {
                    case '1':
                        con = `#!/bin/bash

anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\\\`$anynowtime\\\`][PID:$$]"

##### 可在脚本开始运行时调用，打印当时的时间戳及PID。
function job_start
{
    echo "\`eval $NOW\` job_start"
}

##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。
function job_success
{
    MSG="$*"
    echo "\`eval $NOW\` job_success:[$MSG]"
    exit 0
}

##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。
function job_fail
{
    MSG="$*"
    echo "\`eval $NOW\` job_fail:[$MSG]"
    exit 1
}

job_start

###### 可在此处开始编写您的脚本逻辑代码
###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值
###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败`
                        break
                    case '2':
                        con = `@echo on
setlocal enabledelayedexpansion
call:job_start

REM 可在此处开始编写您的脚本逻辑代码
REM 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值
REM 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败



REM 函数定定义区域，不要把正文写到函数区下面
goto:eof
REM 可在脚本开始运行时调用，打印当时的时间戳及PID。
:job_start
    set cu_time=[%date:~0,10% %time:~0,8%]
    for /F "skip=3 tokens=2" %%i in ('tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"') do (
        set pid=[PID:%%i]
        goto:break
    )
    :break
    echo %cu_time%%pid% job_start
    goto:eof

REM 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。
:job_success
    set cu_time=[%date:~0,10% %time:~0,8%]
    for /F "skip=3 tokens=2" %%i in ('tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"') do (
        set pid=[PID:%%i]
        goto:break
    )
    :break
    echo %cu_time%%pid% job_success:[%*]
    exit 0

REM 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。
:job_fail
    set cu_time=[%date:~0,10% %time:~0,8%]
    for /F "skip=3 tokens=2" %%i in ('tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"') do (
        set pid=[PID:%%i]
        goto:break
    )
    :break
    echo %cu_time%%pid% job_fail:[%*]
    exit 1`
                        break
                    case '3':
                        con = `#!/usr/bin/perl

use strict;

sub job_localtime {
    my @n = localtime();
    return sprintf("%04d-%02d-%02d %02d:%02d:%02d",$n[5]+1900,$n[4]+1,$n[3], $n[2], $n[1], $n[0] );
}

##### 可在脚本开始运行时调用，打印当时的时间戳及PID。
sub job_start {
    print "[",&job_localtime,"][PID:$$] job_start\n";
}

##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。
sub job_success {
    print "[",&job_localtime,"][PID:$$] job_success:[@_]\n";
    exit 0;
}

##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。
sub job_fail {
    print "[",&job_localtime,"][PID:$$] job_fail:[@_]\n";
    exit 1;
}

job_start;

###### 可在此处开始编写您的脚本逻辑代码
###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败
`
                        break
                    case '4':
                        con = `#!/usr/bin/env python
# -*- coding: utf8 -*-

import datetime
import os
import sys

def _now(format="%Y-%m-%d %H:%M:%S"):
    return datetime.datetime.now().strftime(format)

##### 可在脚本开始运行时调用，打印当时的时间戳及PID。
def job_start():
    print "[%s][PID:%s] job_start" % (_now(), os.getpid())

##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。
def job_success(msg):
    print "[%s][PID:%s] job_success:[%s]" % (_now(), os.getpid(), msg)
    sys.exit(0)

##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。
def job_fail(msg):
    print "[%s][PID:%s] job_fail:[%s]" % (_now(), os.getpid(), msg)
    sys.exit(1)

if __name__ == '__main__':

    job_start()

###### 可在此处开始编写您的脚本逻辑代码
###### iJobs中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值
###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败
`
                        break
                    case '5':
                        con = `##### 可在脚本开始运行时调用，打印当时的时间戳及PID。
function job_start
{
    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "[{0}][PID:{1}] job_start" -f $cu_date,$pid
}

##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。
function job_success
{
    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    if($args.count -ne 0)
    {
        $args | foreach {$arg_str=$arg_str + " " + $_}
        "[{0}][PID:{1}] job_success:[{2}]" -f $cu_date,$pid,$arg_str.TrimStart(' ')
    }
    else
    {
        "[{0}][PID:{1}] job_success:[]" -f $cu_date,$pid
    }
    exit 0
}

##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。
function job_fail
{
    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    if($args.count -ne 0)
    {
        $args | foreach {$arg_str=$arg_str + " " + $_}
        "[{0}][PID:{1}] job_fail:[{2}]" -f $cu_date,$pid,$arg_str.TrimStart(' ')
    }
    else
    {
        "[{0}][PID:{1}] job_fail:[]" -f $cu_date,$pid
    }
    exit 1
}

job_start

###### 可在此处开始编写您的脚本逻辑代码
###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值
###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败
`
                        break
                    default:
                        con = ''
                }
                this.handleUpdateElement(name, value)
                this.handleUpdateElement('showContent', con)
            },
            async filterNotExistData (type) {
                try {
                    const url = `/environment/api/user/envnode/${this.projectId}/?page=1&pageSize=100`
                    const res = await this.$ajax.get(url)
                    const list = (res.data.records || res.data || []).map(item => ({
                        ...item
                    }))
                    if (this.element[type].length > 0) {
                        this.element[type].forEach(typeItem => {
                            if (typeItem !== '' && list.filter(item => item.nodeHashId === typeItem).length === 0) {
                                // 删除原数组中的当前项
                                this.element[type].splice(this.element[type].findIndex(iitem => iitem.id === typeItem), 1)
                            }
                        })
                    }
                } catch (e) {
                    console.log(e.message || 'request error')
                }
            }
        }
    }
</script>
