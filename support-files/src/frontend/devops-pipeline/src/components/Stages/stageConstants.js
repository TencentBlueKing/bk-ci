/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

export const ICON_MAP = {
    manualTrigger: 'manual',
    timerTrigger: 'timer',
    codeGitWebHookTrigger: 'git',
    codeSVNWebHookTrigger: 'svn',
    codeGitlabWebHookTrigger: 'gitlab',
    CODE_GIT: 'git',
    CODE_SVN: 'svn',
    CODE_GITLAB: 'gitlab',
    linuxScript: 'script',
    powerScript: 'script',
    linuxPaasCodeCCScript: 'atom-codecc',
    xcodeBuild: 'xcode',
    unity3dBuild: 'unity',
    deployDistribution: 'deploy',
    fileArchive: 'archive',
    ipaFileArchive: 'ipa',
    sendRTXNotify: 'wework',
    sendEmailNotify: 'email',
    sendSmsNotify: 'sms',
    sendWechatNotify: 'weixin',
    gcloud: 'deploy',
    jobExecuteTaskExt: 'deploy',
    jobDevOpsFastPushFile: 'deploy',
    jobDevOpsFastExecuteScript: 'deploy',
    jobDevOpsExecuteTaskExt: 'deploy',
    bcsContainerOp: 'deploy',
    newBcsContainerOp: 'deploy',
    comDistribution: 'deploy',
    acrossProjectDistribution: 'deploy',
    cloudStone: 'deploy',
    metaFileScan: 'order',
    unity3dScan: 'order',
    sensitiveScan: 'order',
    singleArchive: 'atom-artifactory',
    buildPushDockerImage: 'docker-shape',
    pyCompile: 'atom-py',
    buildArchiveGet: 'atom-artifactory',
    customizeArchiveGet: 'atom-artifactory',
    bugly: 'atom-upload',
    wetest: 'atom-test',
    iosCertInstall: 'personal-cert',
    enterpriseSign: 'business-cert',
    wetestElement: 'wetest'
}

const PIPELINE_STATUS = [
    {
        status: 'RUNNING',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'PREPARE_ENV',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'WAITING',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'CANCELED',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'FAILED',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'SUCCEED',
        name: '',
        icon: '',
        color: ''
    },
    {
        status: 'HEARTBEAT_TIMEOUT',
        name: '',
        icon: '',
        color: ''
    }
]
export function getIconByType (type) {
    return ICON_MAP[type] || 'order'
}
