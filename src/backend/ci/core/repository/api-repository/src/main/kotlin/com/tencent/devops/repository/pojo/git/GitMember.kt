package com.tencent.devops.repository.pojo.git

import com.fasterxml.jackson.annotation.JsonProperty

/*
* {
        "id": 11323,
        "username": "git-user1",
        "web_url": "http://git.example.tencent.com/u/git-user1",
        "name": "git-user1",
        "state": "active",
        "avatar_url": "git.example.tencent.com/uploads/user/avatar/111323/a75ba2727c7a409cab1d15dd993149aa.jpg",
        "access_level": 30
}
*
* 组/项目的权限access_level包括：

GUEST = 10
FOLLOWER = 15
REPORTER = 20
DEVELOPER = 30
MASTER = 40
OWNER = 50
*
* */

data class GitMember(
    val id: Int,
    val username: String,
    val state: String,
    @JsonProperty("access_level")
    val accessLevel: Int
)