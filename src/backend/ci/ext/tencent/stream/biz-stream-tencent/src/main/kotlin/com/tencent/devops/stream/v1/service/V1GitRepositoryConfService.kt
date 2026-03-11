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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.constant.StreamConstant
import com.tencent.devops.stream.v1.dao.V1GitCISettingDao
import com.tencent.devops.stream.v1.pojo.V1GitRepositoryConf
import com.tencent.devops.stream.v1.pojo.V1RtxCustomProperty
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class V1GitRepositoryConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val gitCISettingDao: V1GitCISettingDao,
    private val streamBasicSettingService: V1StreamBasicSettingService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1GitRepositoryConfService::class.java)
    }

    fun initGitCISetting(userId: String, gitProjectId: Long): Boolean {
        if (gitCISettingDao.getSetting(dslContext, gitProjectId) == null) {
            val projectInfo = requestGitProjectInfo(gitProjectId) ?: return false
            return saveGitCIConf(
                userId = userId,
                repositoryConf = V1GitRepositoryConf(
                    gitProjectId = gitProjectId,
                    name = projectInfo.name,
                    url = projectInfo.gitSshUrl ?: "",
                    homepage = projectInfo.homepage ?: "",
                    gitHttpUrl = projectInfo.gitHttpsUrl ?: "",
                    gitSshUrl = projectInfo.gitSshUrl ?: "",
                    enableCi = false,
                    env = null,
                    createTime = null,
                    updateTime = null,
                    projectCode = null,
                    limitConcurrentJobs = null,
                    rtxCustomProperty = V1RtxCustomProperty(true, setOf()),
                    rtxGroupProperty = null,
                    emailProperty = null,
                    onlyFailedNotify = true
                )
            )
        } else {
            return true
        }
    }

    fun updateGitCISetting(gitProjectId: Long) {
        val projectInfo = requestGitProjectInfo(gitProjectId)
        if (projectInfo != null) gitCISettingDao.updateSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            gitProjectName = projectInfo.name,
            url = projectInfo.gitSshUrl ?: "",
            homePage = projectInfo.homepage ?: "",
            httpUrl = projectInfo.gitHttpsUrl ?: "",
            sshUrl = projectInfo.gitSshUrl ?: ""
        )
    }

    fun disableGitCI(gitProjectId: Long): Boolean {
        if (gitCISettingDao.getSetting(dslContext, gitProjectId) == null) {
            logger.info("git repo not exists.")
            return false
        }
        gitCISettingDao.enableGitCI(dslContext, gitProjectId, false)
        return true
    }

    fun getGitCIConf(gitProjectId: Long): V1GitRepositoryConf? {
        val repo = streamBasicSettingService.getGitCIConf(gitProjectId) ?: return null
        with(repo) {
            return V1GitRepositoryConf(
                gitProjectId = gitProjectId,
                name = name,
                url = url,
                homepage = homepage,
                gitHttpUrl = gitHttpUrl,
                gitSshUrl = gitSshUrl,
                enableCi = enableCi,
                buildPushedBranches = buildPushedBranches,
                limitConcurrentJobs = null,
                buildPushedPullRequest = buildPushedPullRequest,
                env = null,
                projectCode = projectCode,
                rtxCustomProperty = null,
                emailProperty = null,
                rtxGroupProperty = null,
                enableMrBlock = enableMrBlock,
                createTime = createTime,
                updateTime = updateTime
            )
        }
    }

    fun saveGitCIConf(userId: String, repositoryConf: V1GitRepositoryConf): Boolean {
        logger.info("save git ci conf, repositoryConf: $repositoryConf")
        val gitRepoConf = gitCISettingDao.getSetting(dslContext, repositoryConf.gitProjectId)

        // gitRepoConf为null表示新项目
        val projectCode = if (gitRepoConf?.projectCode == null) {
            // 根据url截取group + project的完整路径名称
            var gitProjectName = if (repositoryConf.gitHttpUrl.isNotBlank()) {
                GitUtils.getDomainAndRepoName(repositoryConf.gitHttpUrl).second
            } else {
                repositoryConf.name
            }
            // 可能存在group多层嵌套的情况:a/b/c/d/e/xx.git，超过t_project表的设置长度64，默认只保存后64位的长度
            if (gitProjectName.length > StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                gitProjectName = gitProjectName.substring(
                    gitProjectName.length -
                        StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH,
                    gitProjectName.length
                )
            }

            val projectResult = client.get(ServiceTxProjectResource::class).createGitCIProject(
                gitProjectId = repositoryConf.gitProjectId,
                userId = userId,
                gitProjectName = gitProjectName
            )
            if (projectResult.isNotOk()) {
                throw RuntimeException("Create git ci project in devops failed, msg: ${projectResult.message}")
            }
            projectResult.data!!.projectCode
        } else {
            gitRepoConf.projectCode
        }
        gitCISettingDao.saveSetting(dslContext, repositoryConf, projectCode!!)
        return true
    }

    private fun requestGitProjectInfo(gitProjectId: Long): GitCIProjectInfo? {
        val serviceGitResource = client.getScm(ServiceGitResource::class)
        val accessToken = serviceGitResource.getToken(gitProjectId).data?.accessToken ?: return null
        return serviceGitResource.getProjectInfo(accessToken, gitProjectId).data
    }
}
