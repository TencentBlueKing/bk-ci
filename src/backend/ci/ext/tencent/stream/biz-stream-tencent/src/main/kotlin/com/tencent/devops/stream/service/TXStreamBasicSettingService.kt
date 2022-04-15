/*
 *
 *  * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *  *
 *  * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *  *
 *  * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *  *
 *  * A copy of the MIT License is included in this file.
 *  *
 *  *
 *  * Terms of the MIT License:
 *  * ---------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 *  * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.stream.service

import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.ProjectDeptInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.constant.StreamConstant
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamGitProjectInfoWithProject
import com.tencent.devops.stream.util.GitCommonUtils
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
    private val streamGitTransferService: StreamGitTransferService
) : StreamBasicSettingService(
    dslContext = dslContext,
    client = client,
    streamBasicSettingDao = streamBasicSettingDao,
    pipelineResourceDao = pipelineResourceDao,
    streamGitTransferService = streamGitTransferService
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
            logger.info("git repo not exists.")
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

    // 更新项目组织架构信息
    fun updateProjectOrganizationInfo(
        projectId: String,
        userId: String
    ): UserDeptDetail? {
        val userResult =
            client.get(ServiceTxUserResource::class).get(userId)
        val userUpdateInfo = if (userResult.isNotOk()) {
            logger.error("Update git ci project in devops failed, msg: ${userResult.message}")
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

    fun updateOauthSetting(gitProjectId: Long, userId: String, oauthUserId: String) {
        streamBasicSettingDao.updateOauthSetting(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            userId = userId,
            oauthUserId = oauthUserId
        )
    }

    // TODO("需要等待CORE接口补全后再判断是否需要修改")
    override fun saveStreamConf(userId: String, setting: StreamBasicSetting): Boolean {
        logger.info("save git ci conf, repositoryConf: $setting")
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

    fun fixProjectNameSpace(): Int {
        var count = 0
        var currProjects = streamBasicSettingDao.getProjectNoNameSpace(dslContext)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                refreshNameSpace(it)
                count++
            }
            logger.info("fixProjectNameSpace project ${currProjects.map { it.id }.toList()}, fixed count: $count")
            Thread.sleep(100)
            currProjects = streamBasicSettingDao.getProjectNoNameSpace(dslContext)
        }
        logger.info("fixProjectNameSpace finished count: $count")
        return count
    }

    // 更新时同步更新蓝盾项目名称
    // TODO("需要等待CORE接口补全后再判断是否需要修改")
    override fun refreshSetting(userId: String, gitProjectId: Long): Boolean {
        val projectInfo = requestGitProjectInfo(gitProjectId) ?: return false
        return updateProjectInfo(userId, projectInfo)
    }

    // TODO("需要等待CORE接口补全后再判断是否需要修改")
    override fun updateProjectInfo(
        userId: String,
        projectInfo: StreamGitProjectInfoWithProject
    ): Boolean {
        val oldData = streamBasicSettingDao.getSetting(dslContext, projectInfo.gitProjectId) ?: return false

        streamBasicSettingDao.updateInfoSetting(
            dslContext = dslContext,
            gitProjectId = projectInfo.gitProjectId,
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

        if (oldData.name != projectInfo.name) {
            try {
                client.get(ServiceTxProjectResource::class).updateProjectName(
                    userId = userId,
                    projectCode = GitCommonUtils.getCiProjectId(projectInfo.gitProjectId),
                    projectName = projectInfo.name
                )
            } catch (e: Throwable) {
                logger.error("update bkci project name error :${e.message}")
                return false
            }
        }
        return true
    }

    private fun refreshNameSpace(it: TGitBasicSettingRecord) {
        try {
            val projectResult = requestGitProjectInfo(it.id)
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
            logger.error("refreshNameSpace | Update git ci project in devops failed, msg: ${t.message}")
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

    private fun requestGitProjectInfo(gitProjectId: Long): StreamGitProjectInfoWithProject? {
        return try {
            streamGitTransferService.getGitProjectInfo(gitProjectId.toString(), null)
        } catch (e: Throwable) {
            logger.error("requestGitProjectInfo, msg: ${e.message}")
            return null
        }
    }

    /**可能存在工蜂侧项目名称删除后，新建同名项目，这时候开启CI就会出现插入project表同名冲突失败的情况,
     * 根据传入项目gitProjectName查询t_project表，获取projectId，调用StreamScmService::getProjectInfo获取工蜂项目信息：
     * case:项目存在，并且项目group/project跟入参不一致，说明该projectId的项目信息已修改，需更新同步到t_projec表；
     * case:项目存在，并且项目group/project跟入参一致(工蜂侧做项目group/名称唯一性保障,理论不会出现);
     * case:项目不存在，说明该项目ID已经在工蜂侧删除，则更改该projectID对应的project_name为xxx_时间戳_delete;
     */
    // TODO("需要等待CORE接口补全后再判断是否需要修改")
    override fun checkSameGitProjectName(userId: String, projectName: String) {

        // sp1:根据gitProjectName调用project接口获取t_project信息
        val bkProjectResult = client.get(ServiceTxProjectResource::class).getProjectInfoByProjectName(
            userId = userId,
            projectName = projectName
        ) ?: return

        // sp2:如果已有同名项目，则根据project_id 调用scm接口获取git上的项目信息
        val projectId = bkProjectResult.data!!.projectId.removePrefix(projectPrefix)
        val gitProjectResult = requestGitProjectInfo(projectId.toLong())
        // 如果工蜂存在该项目信息
        if (null != gitProjectResult) {
            // sp3:比对gitProjectinfo的project_name跟入参的gitProjectName对比是否同名，注意gitProjectName这里包含了group信息，拆解开。
            val projectNameFromGit = gitProjectResult.name
            val projectNameFromPara = projectName.substring(projectName.lastIndexOf("/") + 1)

            if (projectNameFromGit.isNotEmpty() && projectNameFromPara.isNotEmpty() &&
                projectNameFromPara != projectNameFromGit
            ) {
                // 项目已修改名称，更新项目信息，包含setting + project表
                refreshSetting(userId, projectId.toLong())
            }
            return
        }

        // 工蜂不存在，则更新t_project表的project_name加上xxx_时间戳_delete,考虑到project_name的长度限制(64),只取时间戳后3位
        try {
            val timeStamp = System.currentTimeMillis().toString()
            var deletedProjectName = "${projectName}_${timeStamp.substring(timeStamp.length - 3)}_delete"
            if (deletedProjectName.length > StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH) {
                deletedProjectName = deletedProjectName.substring(
                    deletedProjectName.length -
                        StreamConstant.STREAM_MAX_PROJECT_NAME_LENGTH
                )
            }
            client.get(ServiceTxProjectResource::class).updateProjectName(
                userId = userId,
                projectCode = GitCommonUtils.getCiProjectId(projectId.toLong()),
                projectName = deletedProjectName
            )
        } catch (e: Throwable) {
            logger.error("update bkci project name error :${e.message}")
        }
    }
}
