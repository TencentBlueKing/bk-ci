package com.tencent.devops.common.pipeline.pojo.bcs

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
