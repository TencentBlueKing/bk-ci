package com.tencent.devops.dispatch.kubernetes.pojo

/*
{
    "workspaceId": "remoting-ruotian12138",
    "userId": "ruotiantang",
    "gitRepo": {
        "gitRepoName": "go-test",
        "gitRepoRef": "master"
    },
    "gitUsername": "ruotiantang",
    "gitEmail": "ruotiantang@tencent.com",
    "userFiles": [
        {
            "from": "/data/apps/a.yaml",
            "to": "/data/landun/workspace/go-test"
        }
    ]
}
 */
data class KubernetesWorkspaceUrlRsp(
    val webVscodeUrl: String,
    val sshUrl: String,
    val apiUrl: String
)
