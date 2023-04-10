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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.constant.I18NConstant.BK_NEED_SUPPLEMEN
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.ProjectDeptInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamConstant
import com.tencent.devops.stream.constant.StreamMessageCode.BK_NEED_SUPPLEMEN
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TXStreamBasicSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitTransferService: StreamGitTransferService,
    private val tokenService: StreamGitTokenService,
    private val streamScmService: StreamScmService,
    private val streamGitConfig: StreamGitConfig
) : StreamBasicSettingService(
    dslContext = dslContext,
    client = client,
    streamBasicSettingDao = streamBasicSettingDao,
    pipelineResourceDao = pipelineResourceDao,
    streamGitTransferService = streamGitTransferService,
    streamGitConfig = streamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TXStreamBasicSettingService::class.java)
        private const val projectPrefix = "git_"
    }

    override fun updateProjectSetting(
        gitProjectId: Long,
        userId: String?,
        buildPushedBranches: Boolean?,
        buildPushedPullRequest: Boolean?,
        enableMrBlock: Boolean?,
        enableCi: Boolean?,
        authUserId: String?,
        enableCommitCheck: Boolean?,
        enableMrComment: Boolean?
    ): Boolean {
        val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null) {
            logger.warn("TXStreamBasicSettingService|updateProjectSetting|git repo not exists.")
            return false
        }

        val userUpdateInfo = updateProjectOrganizationInfo(gitProjectId.toString(), userId ?: setting.enableUserId)
        userUpdateInfo?.let {
            setting.creatorBgName = userUpdateInfo.bgName
            setting.creatorDeptName = userUpdateInfo.deptName
            setting.creatorCenterName = userUpdateInfo.centerName
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
            creatorCenterName = setting.creatorCenterName,
            enableCommitCheck = enableCommitCheck,
            pathWithNamespace = setting.pathWithNamespace,
            nameWithNamespace = setting.nameWithNamespace,
            enableMrComment = enableMrComment
        )
        return true
    }

    // 更新项目组织架构信息
    fun updateProjectOrganizationInfo(
        projectId: String,
        userId: String
    ): UserDeptDetail? {
        val userResult =
            client.get(ServiceTxUserResource::class).get(userId)
        val userUpdateInfo = if (userResult.isNotOk()) {
            logger.warn("TXStreamBasicSettingService|updateProjectOrganizationInfo|msg=${userResult.message}")
            // 如果userId是公共账号则tof接口获取不到用户信息，需调用User服务获取信息
            val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data ?: return null
            userInfo
        } else {
            val userInfo = userResult.data!!
            userInfo
        }
        // 更新项目的组织架构信息
        updateProjectInfo(
            userId = userId,
            projectId = GitCIUtils.GITLABLE + projectId,
            userDeptDetail = userUpdateInfo
        )
        return userUpdateInfo
    }

    // 更新项目信息
    fun updateProjectInfo(userId: String, projectId: String, userDeptDetail: UserDeptDetail) {
        client.get(ServiceTxProjectResource::class).bindProjectOrganization(
            userId = userId,
            projectCode = projectId,
            projectDeptInfo = ProjectDeptInfo(
                bgId = userDeptDetail.bgId,
                bgName = userDeptDetail.bgName,
                deptId = userDeptDetail.deptId,
                deptName = userDeptDetail.deptName,
                centerId = userDeptDetail.centerId,
                centerName = userDeptDetail.centerName
            )
        )
    }

    fun updateOauthSetting(gitProjectId: Long, userId: String, oauthUserId: String) {
        streamBasicSettingDao.updateOauthSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            userId = userId,
            oauthUserId = oauthUserId
        )
    }

    override fun saveStreamConf(userId: String, setting: StreamBasicSetting): Boolean {
        logger.info("TXStreamBasicSettingService|saveStreamConf|setting|$setting")
        val gitRepoConf = streamBasicSettingDao.getSetting(dslContext, setting.gitProjectId)
        if (gitRepoConf?.projectCode == null) {

            // 根据url截取group + project的完整路径名称
            var gitProjectName = GitUtils.getDomainAndRepoName(setting.gitHttpUrl).second

            // 可能存在group多层嵌套的情况:a/b/c/d/e/xx.git，超过t_project表的设置长度64，默认只保存后64位的长度
            if (gitProjectName.length > StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                gitProjectName = gitProjectName.substring(
                    gitProjectName.length -
                        StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH,
                    gitProjectName.length
                )
            }

            // 增加判断可能存在工蜂侧项目名称删除后，新建同名项目，这时候开启CI就会出现插入project表同名冲突失败的情况,
            checkSameGitProjectName(userId, gitProjectName)
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
                logger.warn("TXStreamBasicSettingService|saveStreamConf|error=${projectResult.message}")
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
            logger.info(
                "TXStreamBasicSettingService|fixProjectInfo" +
                    "|project|${currProjects.map { it.id }.toList()}|fixed count|$count"
            )
            Thread.sleep(100)
            currProjects = streamBasicSettingDao.getProjectNoHttpUrl(dslContext)
        }
        logger.info("TXStreamBasicSettingService|fixProjectInfo|finished count|$count")
        return count
    }

    fun fixProjectNameSpace(): Int {
        var count = 0
        var currProjects = streamBasicSettingDao.getProjectNoNameSpace(dslContext)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                refreshNameSpace(it)
                count++
            }
            logger.info(
                "TXStreamBasicSettingService|fixProjectNameSpace" +
                    "|project|${currProjects.map { it.id }.toList()}|fixed count|$count"
            )
            Thread.sleep(100)
            currProjects = streamBasicSettingDao.getProjectNoNameSpace(dslContext)
        }
        logger.info("TXStreamBasicSettingService|fixProjectNameSpace|finished count|$count")
        return count
    }

    private fun refreshNameSpace(it: TGitBasicSettingRecord) {
        try {
            val projectResult = requestGitProjectInfo(it.id, "")
            if (projectResult != null) {
                streamBasicSettingDao.fixProjectNameSpace(
                    dslContext = dslContext,
                    gitProjectId = it.id,
                    pathWithNamespace = projectResult.pathWithNamespace ?: "",
                    nameWithNamespace = projectResult.nameWithNamespace
                )
            } else {
                // 说明存量数据在工蜂处已丢失
                streamBasicSettingDao.fixProjectNameSpace(
                    dslContext = dslContext,
                    gitProjectId = it.id,
                    pathWithNamespace = "",
                    nameWithNamespace = ""
                )
            }
        } catch (t: Throwable) {
            logger.warn("refreshNameSpace | Update git ci project in devops failed, msg: ${t.message}")
        }
    }

    private fun refresh(it: TGitBasicSettingRecord) {
        try {
            val projectResult = requestGitProjectInfo(it.id, "")
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
            logger.warn("TXStreamBasicSettingService|refresh|msg=${t.message}")
        }
    }

    override fun requestGitProjectInfo(gitProjectId: Long, userId: String): StreamGitProjectInfoWithProject? {
        return when (streamGitConfig.getScmType()) {
            ScmType.CODE_GIT -> try {
                val accessToken = tokenService.getToken(gitProjectId)
                streamScmService.getProjectInfo(accessToken, gitProjectId.toString(), useAccessToken = true)?.let {
                    StreamGitProjectInfoWithProject(
                        gitProjectId = it.gitProjectId,
                        name = it.name,
                        homepage = it.homepage,
                        gitHttpUrl = it.gitHttpUrl.replace("https", "http"),
                        gitHttpsUrl = it.gitHttpsUrl,
                        gitSshUrl = it.gitSshUrl,
                        nameWithNamespace = it.nameWithNamespace,
                        pathWithNamespace = it.pathWithNamespace,
                        defaultBranch = it.defaultBranch,
                        description = it.description,
                        avatarUrl = it.avatarUrl,
                        routerTag = null
                    )
                }
            } catch (e: Throwable) {
                logger.warn("TXStreamBasicSettingService|requestGitProjectInfo|error=${e.message}")
                return null
            }
            else -> TODO(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_NEED_SUPPLEMEN,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    fun updateEnableUserIdByNewUser(
        oldUserId: String,
        newUserId: String,
        limitNumber: Int
    ): Int {
        val idList = streamBasicSettingDao.getSettingByEnableUserId(
            dslContext = dslContext,
            enableUserId = oldUserId,
            limit = limitNumber
        ).map { it.value1() }
        return streamBasicSettingDao.updateEnableUserIdByIds(
            dslContext = dslContext,
            newUserId = newUserId,
            idList = idList
        )
    }
    fun updateGitDomain(
        oldGitDomain: String,
        newGitDomain: String,
        limitNumber: Int
    ): Int {
        val idList = streamBasicSettingDao.getProjectByGitDomain(
            dslContext = dslContext,
            gitDomain = oldGitDomain,
            limit = limitNumber
        ).map { it.value1() }
        return streamBasicSettingDao.updateGitDomainByIds(
            dslContext = dslContext,
            oldGitDomain = oldGitDomain,
            newGitDomain = newGitDomain,
            idList = idList
        )
    }
}
