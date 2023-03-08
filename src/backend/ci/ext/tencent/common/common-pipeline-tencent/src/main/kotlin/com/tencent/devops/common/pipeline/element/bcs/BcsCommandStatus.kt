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

package com.tencent.devops.common.pipeline.element.bcs

/*{
    "data": {
    "id": "Application-jia1-application-bcs2-1547711064982826994",
    "createTime": 1547711064,
    "spec": {
    "commandTargetRef": {
    "kind": "Application",
    "id": "",
    "name": "application-bcs2",
    "namespace": "jia1"
},
    "taskgroups": [],
    "command": [
    "top",
    "vi"
    ],
    "env": [],
    "user": "root",
    "workingDir": "/root",
    "privileged": false,
    "reserveTime": 24607
},
    "status": {
    "taskgroups": [
    {
        "taskgroupId": "0.application-bcs2.jia1.10011.1547710481446933742",
        "tasks": [
        {
            "taskId": "1547710481446933742.0.0.application-bcs2.jia1.10011",
            "status": "finish",
            "message": "",
            "commInspect": {
            "exitCode": 0,
            "stdout": "\ttop: procps version 3.2.8\nusage:\ttop -hv | -abcHimMsS -d delay -n iterations [-u user | -U user] -p pid [,pid ...]\n\n",
            "stderr": ""
        }
        }
        ]
    }
    ]
}
},
    "code": 0,
    "message": "OK",
    "request_id": "0a1a142c5d19baa3ce7ee74cc2b4f74d"
}*/

data class BcsCommandStatus(
    var data: BcsCommandStatusData,
    var code: Int,
    var message: String?,
    var request_id: String
)

data class BcsCommandStatusData(
    var id: String?,
    var createTime: Int?,
    var spec: Spec?,
    var status: Status
)

data class Spec(
    var commandTargetRef: CommandTargetRef?,
    var taskgroups: List<Any>?,
    var Command: List<String>?,
    var env: List<Any>?,
    var user: String?,
    var workingDir: String?,
    var privileged: Boolean?,
    var reserveTime: Int?
)

data class CommandTargetRef(
    var kind: String?,
    var id: String?,
    var name: String?,
    var namespace: String?
)

data class Status(
    var taskgroups: List<Taskgroup>?
)

data class Taskgroup(
    var taskgroupId: String?,
    var tasks: List<Task>?
)

data class Task(
    var taskId: String,
    var status: String,
    var message: String?,
    var commInspect: CommInspect?
)

data class CommInspect(
    var exitCode: Int,
    var stdout: String,
    var stderr: String
)
