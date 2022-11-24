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
    private val workspaceId: String,
    private val userId: String,
    private val gitRepo: GitRepo,
    private val gitUserName: String,
    private val gitEmail: String,
    private val userFiles: List<UserFile>
)

data class GitRepo(
    private val gitRepoName: String,
    private val gitRepoRef: String
)

data class UserFile(
    private val from: String,
    private val to: String
)
