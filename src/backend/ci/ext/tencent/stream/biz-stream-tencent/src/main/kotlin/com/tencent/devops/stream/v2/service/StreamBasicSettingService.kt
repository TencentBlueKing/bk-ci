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
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.common.exception.GitCINoEnableException
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.constant.GitCIConstant
import com.tencent.devops.stream.utils.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamBasicSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val tokenService: StreamGitTokenService,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamBasicSettingService::class.java)
    }

    fun updateProjectSetting(
        gitProjectId: Long,
        userId: String? = null,
        buildPushedBranches: Boolean? = null,
        buildPushedPullRequest: Boolean? = null,
        enableMrBlock: Boolean? = null,
        enableCi: Boolean? = null,
        authUserId: String? = null
    ): Boolean {
        val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null) {
            logger.info("git repo not exists.")
            return false
        }
        if (!userId.isNullOrBlank()) {
            val projectResult =
                client.get(ServiceTxUserResource::class).get(userId)
            if (projectResult.isNotOk()) {
                logger.error("Update git ci project in devops failed, msg: ${projectResult.message}")
            } else {
                val userInfo = projectResult.data!!
                setting.creatorBgName = userInfo.bgName
                setting.creatorDeptName = userInfo.deptName
                setting.creatorCenterName = userInfo.centerName
            }
        }
        streamBasicSettingDao.updateProjectSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            userId = userId,
            buildPushedBranches = buildPushedBranches,
            buildPushedPullRequest = buildPushedPullRequest,
            enableMrBlock = enableMrBlock,
            enableCi = enableCi,
            authUserId = authUserId,
            creatorBgName = setting.creatorBgName,
            creatorDeptName = setting.creatorDeptName,
            creatorCenterName = setting.creatorCenterName
        )
        return true
    }

    fun updateOauthSetting(gitProjectId: Long, userId: String, oauthUserId: String) {
        streamBasicSettingDao.updateOauthSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            userId = userId,
            oauthUserId = oauthUserId
        )
    }

    fun getGitCIConf(gitProjectId: Long): GitCIBasicSetting? {
        return streamBasicSettingDao.getSetting(dslContext, gitProjectId)
    }

    fun getGitCIBasicSettingAndCheck(gitProjectId: Long): GitCIBasicSetting {
        return streamBasicSettingDao.getSetting(dslContext, gitProjectId)
            ?: throw GitCINoEnableException(gitProjectId.toString())
    }

    fun initGitCIConf(
        userId: String,
        projectId: String,
        gitProjectId: Long,
        enabled: Boolean
    ): Boolean {
        val projectInfo = requestGitProjectInfo(gitProjectId)

        run back@{
            return saveGitCIConf(
                userId,
                GitCIBasicSetting(
                    gitProjectId = gitProjectId,
                    name = projectInfo?.name ?: return@back,
                    url = projectInfo.gitSshUrl ?: return@back,
                    homepage = projectInfo.homepage ?: return@back,
                    gitHttpUrl = projectInfo.gitHttpsUrl ?: return@back,
                    gitSshUrl = projectInfo.gitSshUrl ?: return@back,
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
                    creatorBgName = null,
                    gitProjectDesc = projectInfo.description,
                    gitProjectAvatar = projectInfo.avatarUrl,
                    lastCiInfo = null
                )
            )
        }
        logger.warn("initGitCIConf: $gitProjectId  info: $projectInfo")
        throw RuntimeException("Create git ci project in devops failed, msg: get project info from git error")
    }

    fun saveGitCIConf(userId: String, setting: GitCIBasicSetting): Boolean {
        logger.info("save git ci conf, repositoryConf: $setting")
        val gitRepoConf = streamBasicSettingDao.getSetting(dslContext, setting.gitProjectId)
        if (gitRepoConf?.projectCode == null) {

            // 根据url截取group + project的完整路径名称
            var gitProjectName = GitUtils.getDomainAndRepoName(setting.gitHttpUrl).second

            // 可能存在group多层嵌套的情况:a/b/c/d/e/xx.git，超过t_project表的设置长度64，默认只保存后64位的长度
            if (gitProjectName.length > GitCIConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                gitProjectName = gitProjectName.substring(
                    gitProjectName.length -
                            GitCIConstant.STREAM_MAX_PROJECT_NAME_LENGTH, gitProjectName.length
                )
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
            streamBasicSettingDao.saveSetting(dslContext, setting, projectInfo.projectCode)
        } else {
            val projectResult = client.get(ServiceTxUserResource::class).get(gitRepoConf.enableUserId)
            if (projectResult.isNotOk()) {
                logger.error("Update git ci project in devops failed, msg: ${projectResult.message}")
                return false
            }
            val userInfo = projectResult.data!!
            setting.creatorBgName = userInfo.bgName
            setting.creatorDeptName = userInfo.deptName
            setting.creatorCenterName = userInfo.centerName
            streamBasicSettingDao.saveSetting(dslContext, setting, gitRepoConf.projectCode!!)
        }
        return true
    }

    fun fixProjectInfo(): Int {
        var count = 0
        var currProjects = streamBasicSettingDao.getProjectNoHttpUrl(dslContext)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                refresh(it)
                count++
            }
            logger.info("fixProjectInfo project ${currProjects.map { it.id }.toList()}, fixed count: $count")
            Thread.sleep(100)
            currProjects = streamBasicSettingDao.getProjectNoHttpUrl(dslContext)
        }
        logger.info("fixProjectInfo finished count: $count")
        return count
    }

    // 更新时同步更新蓝盾项目名称
    fun refreshSetting(userId: String, gitProjectId: Long) {
        val projectInfo = requestGitProjectInfo(gitProjectId) ?: return
        updateProjectInfo(userId, projectInfo)
    }

    fun updateProjectInfo(userId: String, projectInfo: GitCIProjectInfo) {
        run back@{
            streamBasicSettingDao.updateInfoSetting(
                dslContext = dslContext,
                gitProjectId = projectInfo.gitProjectId,
                gitProjectName = projectInfo.name,
                url = projectInfo.gitSshUrl ?: return@back,
                homePage = projectInfo.homepage ?: return@back,
                httpUrl = projectInfo.gitHttpsUrl ?: return@back,
                sshUrl = projectInfo.gitSshUrl ?: return@back,
                desc = projectInfo.description,
                avatar = projectInfo.avatarUrl
            )
        }
        val oldData = streamBasicSettingDao.getSetting(dslContext, projectInfo.gitProjectId) ?: return
        if (oldData.name != projectInfo.name) {
            try {
                client.get(ServiceTxProjectResource::class).updateProjectName(
                    userId = userId,
                    projectCode = GitCommonUtils.getCiProjectId(projectInfo.gitProjectId),
                    projectName = projectInfo.name
                )
            } catch (e: Throwable) {
                logger.error("update bkci project name error :${e.message}")
            }
        }
    }

    fun getMaxId(
        gitProjectIdList: List<Long>? = null
    ): Long {
        return streamBasicSettingDao.getMaxId(dslContext, gitProjectIdList)
    }

    fun getBasicSettingList(
        gitProjectIdList: List<Long>? = null,
        minId: Long? = null,
        maxId: Long? = null
    ): List<Long>? {
        val basicSettingRecords = streamBasicSettingDao.getBasicSettingList(
            dslContext = dslContext,
            gitProjectIdList = gitProjectIdList,
            minId = minId,
            maxId = maxId
        )
        return if (basicSettingRecords.isEmpty()) {
            null
        } else {
            val idList = mutableListOf<Long>()
            basicSettingRecords.forEach { record ->
                idList.add(
                    record.id
                )
            }
            idList
        }
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
                streamBasicSettingDao.fixProjectInfo(
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
            val accessToken = tokenService.getToken(gitProjectId)
            streamScmService.getProjectInfo(accessToken, gitProjectId.toString(), useAccessToken = true)
        } catch (e: Throwable) {
            logger.error("requestGitProjectInfo, msg: ${e.message}")
            return null
        }
    }
}
