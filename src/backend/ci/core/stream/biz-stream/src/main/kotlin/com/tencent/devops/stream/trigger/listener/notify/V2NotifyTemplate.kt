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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.stream.trigger.listener.notify

import com.tencent.devops.common.pipeline.enums.BuildStatus

// v2 Stream的默认通知模板
object V2NotifyTemplate {

    fun getEmailTitle(
        status: BuildStatus,
        projectName: String,
        branchName: String,
        pipelineName: String,
        buildNum: String
    ): String {
        val state = when {
            status.isSuccess() -> "run successes"
            status.isCancel() -> "run cancel"
            else -> "run failed"
        }
        return """
            [$projectName][$branchName] $pipelineName #$buildNum $state
        """
    }

    fun getEmailContent(
        status: BuildStatus,
        projectName: String,
        branchName: String,
        pipelineName: String,
        buildNum: String,
        startTime: String,
        totalTime: String,
        trigger: String,
        commitId: String,
        webUrl: String
    ): String {
        val state = when {
            status.isSuccess() -> "run successes"
            status.isCancel() -> "run cancel"
            else -> "run failed"
        }
        return """
            <!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>

<body style="color: #666;padding: 20px;">
    <h3 style="font-weight: normal;margin: 0;padding: 0;line-height: 40px;">[$projectName][$branchName]
        $pipelineName #$buildNum $state </h3>
    <ul style="margin: 7px 0;padding: 0;">
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Build num：
            </span><span>#$buildNum</span> </li>
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Start Time：
            </span><span>$startTime</span> </li>
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Total duration：
            </span><span>$totalTime</span> </li>
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Trigger：
            </span><span>$trigger</span> </li>
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Branch：
            </span><span>$branchName</span> </li>        
        <li style="list-style: none;display: block;line-height: 30px;"> <span
                style="width: 130px;display: inline-block;text-align: right;">Commit： </span><a href="$webUrl"
                style="color: #3a84ff;">$commitId</a> </li>
    </ul> <a href="$webUrl" style="color: #3a84ff;line-height: 30px;">查看详情</a>
</body>

</html>
"""
    }
}
