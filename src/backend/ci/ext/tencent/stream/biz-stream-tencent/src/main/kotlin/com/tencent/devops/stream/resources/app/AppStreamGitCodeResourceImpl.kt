package com.tencent.devops.stream.resources.app

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.stream.api.app.AppStreamGitCodeResource
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.TXStreamBasicSettingService
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.stream.v2.service.StreamPipelineBranchService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppStreamGitCodeResourceImpl @Autowired constructor(
    private val permissionService: GitCIV2PermissionService,
    private val streamScmService: StreamScmService,
    private val TXStreamBasicSettingService: TXStreamBasicSettingService,
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
        permissionService.checkGitCIPermission(userId, projectId)
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
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): Result<Page<String>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        return Result(
            streamPipelineBranchService.getProjectBranches(
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                page = page ?: 1,
                pageSize = pageSize ?: 100,
                orderBy = orderBy ?: GitCodeBranchesOrder.UPDATE,
                sort = sort ?: GitCodeBranchesSort.DESC,
                search = search
            )
        )
    }

    // 看是否使用工蜂开启人的OAuth
    private fun getOauthToken(gitProjectId: Long): String {
        val setting = TXStreamBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
        return oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
    }
}
