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

package com.tencent.devops.stream.v2.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.stream.common.exception.GitCINoEnableException
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.constant.GitCIConstant
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIBasicSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBasicSettingService::class.java)
    }

    fun updateProjectSetting(
        gitProjectId: Long,
        buildPushedBranches: Boolean? = null,
        buildPushedPullRequest: Boolean? = null,
        enableMrBlock: Boolean? = null,
        enableCi: Boolean? = null,
        enableUserId: String? = null
    ): Boolean {
        val setting = gitCIBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null) {
            logger.info("git repo not exists.")
            return false
        }
        if (!enableUserId.isNullOrBlank()) {
            val projectResult =
                client.get(ServiceTxUserResource::class).get(enableUserId)
            if (projectResult.isNotOk()) {
                logger.error("Update git ci project in devops failed, msg: ${projectResult.message}")
            } else {
                val userInfo = projectResult.data!!
                setting.creatorBgName = userInfo.bgName
                setting.creatorDeptName = userInfo.deptName
                setting.creatorCenterName = userInfo.centerName
            }
        }
        gitCIBasicSettingDao.updateProjectSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            buildPushedBranches = buildPushedBranches,
            buildPushedPullRequest = buildPushedPullRequest,
            enableMrBlock = enableMrBlock,
            enableCi = enableCi,
            enableUserId = enableUserId,
            creatorBgName = setting.creatorBgName,
            creatorDeptName = setting.creatorDeptName,
            creatorCenterName = setting.creatorCenterName
        )
        return true
    }

    fun getGitCIConf(gitProjectId: Long): GitCIBasicSetting? {
        return gitCIBasicSettingDao.getSetting(dslContext, gitProjectId)
    }

    fun getGitCIBasicSettingAndCheck(gitProjectId: Long): GitCIBasicSetting {
        return gitCIBasicSettingDao.getSetting(dslContext, gitProjectId)
            ?: throw GitCINoEnableException(gitProjectId.toString())
    }

    fun initGitCIConf(
        userId: String,
        projectId: String,
        gitProjectId: Long,
        enabled: Boolean,
        projectInfo: GitCIProjectInfo
    ): Boolean {
        val httpUrl = if (projectInfo.gitHttpUrl.startsWith("https://")) {
            projectInfo.gitHttpUrl
        } else {
            val projectResult = requestGitProjectInfo(gitProjectId)
            if (projectResult != null) {
                if (projectResult.gitHttpsUrl?.startsWith("https://") == true) {
                    projectResult.gitHttpsUrl
                } else {
                    projectInfo.gitHttpUrl
                }
            } else {
                projectInfo.gitHttpUrl
            }
        } ?: projectInfo.gitHttpUrl

        return saveGitCIConf(
            userId,
            GitCIBasicSetting(
                gitProjectId = gitProjectId,
                name = projectInfo.name,
                url = projectInfo.gitSshUrl ?: "",
                homepage = projectInfo.homepage ?: "",
                gitHttpUrl = httpUrl,
                gitSshUrl = projectInfo.gitSshUrl ?: "",
                enableCi = enabled,
                enableUserId = userId,
                buildPushedBranches = true,
                buildPushedPullRequest = true,
                enableMrBlock = true,
                projectCode = projectId,
                createTime = null,
                updateTime = null,
                creatorCenterName = null,
                creatorDeptName = null,
                creatorBgName = null
            )
        )
    }

    fun saveGitCIConf(userId: String, setting: GitCIBasicSetting): Boolean {
        logger.info("save git ci conf, repositoryConf: $setting")
        val gitRepoConf = gitCIBasicSettingDao.getSetting(dslContext, setting.gitProjectId)
        if (gitRepoConf?.projectCode == null) {

            // 根据url截取group + project的完整路径名称
            var gitProjectName = if (setting?.gitHttpUrl != null) {
                GitUtils.getDomainAndRepoName(setting?.gitHttpUrl).second
            } else {
                setting.name
            }

            // 可能存在group多层嵌套的情况:a/b/c/d/e/xx.git，超过t_project表的设置长度64，默认只保存后64位的长度
            if (gitProjectName?.length > GitCIConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                gitProjectName = gitProjectName.substring(gitProjectName.length - GitCIConstant.STREAM_MAX_PROJECT_NAME_LENGTH, gitProjectName.length)
            }
            val projectResult =
                client.get(ServiceTxProjectResource::class).createGitCIProject(
                    gitProjectId = setting.gitProjectId,
                    userId = userId,
                    gitProjectName = gitProjectName
                )
            if (projectResult.isNotOk()) {
                throw RuntimeException("Create git ci project in devops failed, msg: ${projectResult.message}")
            }
            val projectInfo = projectResult.data!!
            setting.creatorBgName = projectInfo.bgName
            setting.creatorDeptName = projectInfo.deptName
            setting.creatorCenterName = projectInfo.centerName
            gitCIBasicSettingDao.saveSetting(dslContext, setting, projectInfo.projectCode)
        } else {
            val projectResult =
                client.get(ServiceTxUserResource::class).get(gitRepoConf.enableUserId)
            if (projectResult.isNotOk()) {
                logger.error("Update git ci project in devops failed, msg: ${projectResult.message}")
                return false
            }
            val userInfo = projectResult.data!!
            setting.creatorBgName = userInfo.bgName
            setting.creatorDeptName = userInfo.deptName
            setting.creatorCenterName = userInfo.centerName
            gitCIBasicSettingDao.saveSetting(dslContext, setting, gitRepoConf.projectCode!!)
        }
        return true
    }

    fun fixProjectInfo(): Int {
        var count = 0
        var currProjects = gitCIBasicSettingDao.getProjectNoHttpUrl(dslContext)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                refresh(it)
                count++
            }
            logger.info("fixProjectInfo project ${currProjects.map { it.id }.toList()}, fixed count: $count")
            Thread.sleep(100)
            currProjects = gitCIBasicSettingDao.getProjectNoHttpUrl(dslContext)
        }
        logger.info("fixProjectInfo finished count: $count")
        return count
    }

    fun refreshSetting(gitProjectId: Long) {
        val projectInfo = requestGitProjectInfo(gitProjectId)
        if (projectInfo != null) gitCIBasicSettingDao.updateInfoSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            gitProjectName = projectInfo.name,
            url = projectInfo.gitSshUrl ?: "",
            homePage = projectInfo.homepage ?: "",
            httpUrl = projectInfo.gitHttpsUrl ?: "",
            sshUrl = projectInfo.gitSshUrl ?: ""
        )
    }

    private fun refresh(it: TGitBasicSettingRecord) {
        try {
            val projectResult = requestGitProjectInfo(it.id)
            if (projectResult != null) {
                val httpUrl = if (!projectResult.gitHttpsUrl.isNullOrBlank()) {
                    projectResult.gitHttpsUrl!!
                } else {
                    projectResult.gitHttpUrl
                }
                gitCIBasicSettingDao.fixProjectInfo(
                    dslContext = dslContext,
                    gitProjectId = it.id,
                    httpUrl = httpUrl
                )
            }
        } catch (t: Throwable) {
            logger.error("Update git ci project in devops failed, msg: ${t.message}")
        }
    }

    private fun requestGitProjectInfo(gitProjectId: Long): GitCIProjectInfo? {
        return try {
            val accessToken =
                client.getScm(ServiceGitCiResource::class).getToken(gitProjectId.toString()).data!!.accessToken
            client.getScm(ServiceGitCiResource::class)
                .getProjectInfo(accessToken, gitProjectId.toString(), useAccessToken = true).data
        } catch (e: Throwable) {
            logger.error("requestGitProjectInfo, msg: ${e.message}")
            return null
        }
    }
}
