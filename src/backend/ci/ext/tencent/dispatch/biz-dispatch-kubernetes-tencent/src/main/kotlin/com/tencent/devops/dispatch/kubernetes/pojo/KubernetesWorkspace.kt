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
data class KubernetesWorkspace(
    val workspaceId: String,
    val userId: String,
    val gitRepo: GitRepo,
    val gitUserName: String,
    val gitEmail: String,
    val remotingYamlName: String,
    val userFiles: List<UserFile>
)

data class GitRepo(
    val gitRepoName: String,
    val gitRepoRef: String
)

data class UserFile(
    val from: String,
    val to: String
)
