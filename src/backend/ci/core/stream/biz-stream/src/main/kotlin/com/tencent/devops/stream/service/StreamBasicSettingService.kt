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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.thirdPartyAgent.UserThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.AgentBuildDetail
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectTypeEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.common.exception.StreamNoEnableException
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamConstant
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StreamBasicSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitTransferService: StreamGitTransferService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamBasicSettingService::class.java)
    }

    @Value("\${pac.defaultEnable:#{null}}")
    private val defaultEnable: Boolean? = null

    fun listAgentBuilds(
        user: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentBuildDetail> {
        val agentBuilds =
            client.get(UserThirdPartyAgentResource::class).listAgentBuilds(
                userId = user,
                projectId = projectId,
                nodeHashId = nodeHashId,
                page = page,
                pageSize = pageSize
            )
        if (agentBuilds.isNotOk()) {
            logger.warn(
                "StreamBasicSettingService|listAgentBuilds|" +
                    "errors=${agentBuilds.message}"
            )
            throw RuntimeException("get agent builds list in devops failed, msg: ${agentBuilds.message}")
        }
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        val pipelines = pipelineResourceDao.getPipelinesInIds(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineIds = agentBuilds.data!!.records.map { it.pipelineId }.toList().distinct()
        ).associateBy { it.pipelineId }
        val agentBuildDetails = agentBuilds.data!!.records.map {
            it.copy(pipelineName = pipelines[it.pipelineId]?.displayName ?: it.pipelineName)
        }
        return agentBuilds.data!!.copy(records = agentBuildDetails)
    }

    fun updateProjectSetting(
        gitProjectId: Long,
        userId: String? = null,
        buildPushedBranches: Boolean? = null,
        buildPushedPullRequest: Boolean? = null,
        enableMrBlock: Boolean? = null,
        enableCi: Boolean? = null,
        authUserId: String? = null,
        enableCommitCheck: Boolean? = null,
        enableMrComment: Boolean? = null
    ): Boolean {
        val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null) {
            logger.info("StreamBasicSettingService|updateProjectSetting|git repo not exists")
            return false
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

    fun updateProjectReviewSetting(
        gitProjectId: Long,
        userId: String? = null,
        triggerReviewSetting: TriggerReviewSetting
    ): Boolean {
        val setting = streamBasicSettingDao.getSetting(dslContext, gitProjectId)
        if (setting == null) {
            logger.info("git repo not exists.")
            return false
        }
        streamBasicSettingDao.updateProjectReviewSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            triggerReviewSetting = triggerReviewSetting
        )
        return true
    }

    fun getStreamConf(gitProjectId: Long): StreamBasicSetting? {
        return streamBasicSettingDao.getSetting(dslContext, gitProjectId)
    }

    fun getStreamBasicSettingAndCheck(gitProjectId: Long): StreamBasicSetting {
        return streamBasicSettingDao.getSetting(dslContext, gitProjectId)
            ?: throw StreamNoEnableException(gitProjectId.toString())
    }

    fun initStreamConf(
        userId: String,
        projectId: String,
        gitProjectId: Long,
        enabled: Boolean
    ): Boolean {
        val projectInfo = requestGitProjectInfo(gitProjectId, userId)

        run back@{
            return saveStreamConf(
                userId = userId,
                setting = StreamBasicSetting(
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
                    lastCiInfo = null,
                    pathWithNamespace = projectInfo.pathWithNamespace,
                    nameWithNamespace = projectInfo.nameWithNamespace
                )
            )
        }
        logger.warn("StreamBasicSettingService|initStreamConf|$gitProjectId|$projectInfo")
        throw RuntimeException("Create git ci project in devops failed, msg: get project info from git error")
    }

    fun saveStreamConf(userId: String, setting: StreamBasicSetting): Boolean {
        logger.info("StreamBasicSettingService|saveStreamConf|$setting")
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

            // 增加判断可能存在stream 侧项目名称删除后，新建同名项目，这时候开启CI就会出现插入project表同名冲突失败的情况,
            checkSameGitProjectName(userId, gitProjectName)

            val projectCode = GitCommonUtils.getCiProjectId(
                setting.gitProjectId,
                streamGitConfig.getScmType()
            )
            val projectResult = client.get(ServiceProjectResource::class).createExtSystem(
                userId = userId,
                projectInfo = ProjectCreateInfo(
                    projectName = gitProjectName ?: projectCode,
                    englishName = projectCode,
                    projectType = ProjectTypeEnum.SUPPORT_PRODUCT.index,
                    description = "git ci project for git projectId: ${setting.gitProjectId}",
                    bgId = 0L,
                    bgName = "",
                    deptId = 0L,
                    deptName = "",
                    centerId = 0L,
                    centerName = "",
                    secrecy = false,
                    kind = 0,
                    properties = ProjectProperties(PipelineAsCodeSettings(defaultEnable != false))
                ),
                needValidate = false,
                needAuth = false,
                channel = ProjectChannelCode.GITCI
            )
            if (projectResult.isNotOk()) {
                logger.warn("StreamBasicSettingService|saveStreamConf|error=${projectResult.message}")
                throw RuntimeException("Create git ci project in devops failed, msg: ${projectResult.message}")
            }
            val projectInfo = projectResult.data!!
            setting.creatorBgName = projectInfo.bgName
            setting.creatorDeptName = projectInfo.deptName
            setting.creatorCenterName = projectInfo.centerName
            streamBasicSettingDao.saveSetting(dslContext, setting, projectInfo.projectCode)
        }

        return true
    }

    // 更新时同步更新蓝盾项目名称
    fun refreshSetting(userId: String, gitProjectId: Long): Boolean {
        val projectInfo = requestGitProjectInfo(gitProjectId, userId) ?: return false
        return updateProjectInfo(userId, projectInfo)
    }

    fun updateProjectInfo(
        userId: String,
        projectInfo: StreamGitProjectInfoWithProject
    ): Boolean {
        val oldData = streamBasicSettingDao.getSetting(dslContext, projectInfo.gitProjectId) ?: return false

        streamBasicSettingDao.updateInfoSetting(
            dslContext = dslContext,
            gitProjectId = projectInfo.gitProjectId.toLong(),
            gitProjectName = projectInfo.name,
            url = projectInfo.gitSshUrl ?: oldData.gitSshUrl,
            homePage = projectInfo.homepage ?: oldData.homepage,
            httpUrl = projectInfo.gitHttpsUrl ?: oldData.gitHttpUrl,
            sshUrl = projectInfo.gitSshUrl ?: oldData.gitSshUrl,
            desc = projectInfo.description,
            avatar = projectInfo.avatarUrl,
            pathWithNamespace = projectInfo.pathWithNamespace,
            nameWithNamespace = projectInfo.nameWithNamespace
        )

        var newProjectName = projectInfo.nameWithNamespace
        var needToUpdate = false
        if (oldData.name != projectInfo.name) {
            needToUpdate = true
        } else if (oldData.pathWithNamespace != projectInfo.pathWithNamespace) {
            needToUpdate = true
            newProjectName = projectInfo.pathWithNamespace.toString()
        }

        if (needToUpdate) {
            if (newProjectName.length > StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                newProjectName = newProjectName.substring(
                    newProjectName.length -
                        StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH,
                    newProjectName.length
                )
            }
            try {
                client.get(ServiceProjectResource::class).updateProjectName(
                    userId = userId,
                    projectCode = GitCommonUtils.getCiProjectId(
                        projectInfo.gitProjectId,
                        streamGitConfig.getScmType()
                    ),
                    projectName = newProjectName
                )
            } catch (e: Throwable) {
                logger.warn("StreamBasicSettingService|updateProjectInfo|error", e)
                return false
            }
        }

        return true
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

    fun getBasicSettingRecordList(
        gitProjectIdList: List<Long>? = null
    ): List<TGitBasicSettingRecord> {
        return streamBasicSettingDao.getBasicSettingList(
            dslContext = dslContext,
            gitProjectIdList = gitProjectIdList
        )
    }

    protected fun requestGitProjectInfo(gitProjectId: Long, userId: String): StreamGitProjectInfoWithProject? {
        return try {
            streamGitTransferService.getGitProjectInfo(gitProjectId.toString(), userId)
        } catch (e: Throwable) {
            logger.warn("StreamBasicSettingService|requestGitProjectInfo|error", e)
            return null
        }
    }

    /**可能存在stream 侧项目名称删除后，新建同名项目，这时候开启CI就会出现插入project表同名冲突失败的情况,
     * 根据传入项目gitProjectName查询t_project表，获取projectId，调用StreamScmService::getProjectInfo获取stream 项目信息：
     * case:项目存在，并且项目group/project跟入参不一致，说明该projectId的项目信息已修改，需更新同步到t_projec表；
     * case:项目存在，并且项目group/project跟入参一致(stream 侧做项目group/名称唯一性保障,理论不会出现);
     * case:项目不存在，说明该项目ID已经在stream 侧删除，则更改该projectID对应的project_name为xxx_时间戳_delete;
     */
    fun checkSameGitProjectName(userId: String, projectName: String) {

        // sp1:根据gitProjectName调用project接口获取t_project信息  --先注释预留调用
        val bkProjectResult = client.get(ServiceProjectResource::class).getProjectByName(
            userId = userId,
            projectName = projectName
        ).data ?: return

        // sp2:如果已有同名项目，则根据project_id 调用scm接口获取git上的项目信息
        val projectId = GitCommonUtils.getGitProjectId(bkProjectResult.projectId)
        val gitProjectResult = requestGitProjectInfo(projectId, userId)
        logger.info("StreamBasicSettingService|checkSameGitProjectName|$gitProjectResult|$projectName")
        // 如果stream 存在该项目信息
        if (null != gitProjectResult) {
            // sp3:比对gitProjectinfo的project_name跟入参的gitProjectName对比是否同名，注意gitProjectName这里包含了group信息，拆解开。
            // 可能存在用户：只改项目名称，不改路径；只改路径，不改项目名称。
            val projectNameFromGit = gitProjectResult.name
            val pathWithNamespace = gitProjectResult.pathWithNamespace
            val projectNameFromPara = projectName.substring(projectName.lastIndexOf("/") + 1)
            if (projectNameFromPara != projectNameFromGit ||
                pathWithNamespace != projectName
            ) {
                // 项目已修改名称或修改路径，更新项目信息，包含setting + project表
                refreshSetting(userId, projectId)
            }
            return
        }

        // stream 不存在，则更新t_project表的project_name加上xxx_时间戳_delete,考虑到project_name的长度限制(64),只取时间戳后3位
        try {
            val timeStamp = System.currentTimeMillis().toString()
            var deletedProjectName = "${projectName}_${timeStamp.substring(timeStamp.length - 3)}_delete"
            if (deletedProjectName.length > StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                deletedProjectName = deletedProjectName.substring(
                    deletedProjectName.length -
                        StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH
                )
            }

            client.get(ServiceProjectResource::class).updateProjectName(
                userId = userId,
                projectCode = GitCommonUtils.getCiProjectId(
                    projectId.toLong(),
                    streamGitConfig.getScmType()
                ),
                projectName = deletedProjectName
            )
        } catch (e: Throwable) {
            logger.warn(
                "StreamBasicSettingService|checkSameGitProjectName " +
                    "|update bkci project name error :${e.message}"
            )
        }
    }
}
