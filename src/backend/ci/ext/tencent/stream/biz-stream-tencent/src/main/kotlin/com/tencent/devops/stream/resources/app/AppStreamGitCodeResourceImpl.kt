package com.tencent.devops.stream.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.scm.pojo.GitCodeBranchesOrder
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.stream.api.app.AppStreamGitCodeResource
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppStreamGitCodeResourceImpl @Autowired constructor(
    private val permissionService: GitCIV2PermissionService,
    private val streamScmService: StreamScmService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val oauthService: StreamOauthService
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

    // TODO 需要过滤掉不带yaml的分支
    override fun getGitCodeBranches(
        userId: String,
        projectId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: GitCodeBranchesOrder?,
        sort: GitCodeBranchesSort?
    ): Result<List<String>?> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId).toString()
        return Result(
            streamScmService.getProjectBranches(
                token = getOauthToken(gitProjectId.toLong()),
                gitProjectId = gitProjectId,
                page = page,
                pageSize = pageSize,
                orderBy = orderBy,
                sort = sort,
                search = search
            )
        )
    }

    // 看是否使用工蜂开启人的OAuth
    private fun getOauthToken(gitProjectId: Long): String {
        val setting = streamBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
        return oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
    }
}
