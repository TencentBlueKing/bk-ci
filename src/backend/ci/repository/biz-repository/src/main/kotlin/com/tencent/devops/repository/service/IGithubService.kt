package com.tencent.devops.repository.service

import com.tencent.devops.repository.pojo.Project
import io.swagger.annotations.ApiOperation

interface IGithubService {

    fun getGithubOauth(projectId: String, userId: String, repoHashId: String?): String

    @ApiOperation("获取Github仓库列表")
    fun getProject(
        accessToken: String,
        userId: String
    ): List<Project>
}