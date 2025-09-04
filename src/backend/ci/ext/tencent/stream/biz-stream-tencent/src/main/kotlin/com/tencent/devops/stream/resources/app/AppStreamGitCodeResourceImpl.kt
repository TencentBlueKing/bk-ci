package com.tencent.devops.stream.resources.app

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.stream.api.app.AppStreamGitCodeResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import com.tencent.devops.stream.service.StreamOauthService
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.service.TXStreamBasicSettingService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppStreamGitCodeResourceImpl @Autowired constructor(
    private val permissionService: StreamPermissionService,
    private val streamScmService: StreamScmService,
    private val txStreamBasicSettingService: TXStreamBasicSettingService,
    private val oauthService: StreamOauthService,
    private val streamPipelineBranchService: StreamPipelineBranchService
) : AppStreamGitCodeResource {
    override fun getGitCodeCommits(
        userId: String,
        projectId: String,
        filePath: String?,
        branch: String?,
        since: String?,
        until: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<Commit>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkStreamPermission(userId, projectId)
        return Result(
            streamScmService.getCommits(
                token = getOauthToken(gitProjectId),
                gitProjectId = gitProjectId,
                filePath = filePath,
                branch = branch,
                since = since,
                until = until,
                page = page,
                perPage = pageSize
            )
        )
    }

    override fun getGitCodeBranches(
        userId: String,
        projectId: String,
        pipelineId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: StreamBranchesOrder?,
        sort: StreamSortAscOrDesc?
    ): Result<Page<String>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        return Result(
            streamPipelineBranchService.getProjectBranches(
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                page = page ?: 1,
                pageSize = pageSize ?: 100,
                orderBy = orderBy ?: StreamBranchesOrder.UPDATE,
                sort = sort ?: StreamSortAscOrDesc.DESC,
                search = search
            )
        )
    }

    // 看是否使用工蜂开启人的OAuth
    private fun getOauthToken(gitProjectId: Long): String {
        val setting = txStreamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        return oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
    }
}
