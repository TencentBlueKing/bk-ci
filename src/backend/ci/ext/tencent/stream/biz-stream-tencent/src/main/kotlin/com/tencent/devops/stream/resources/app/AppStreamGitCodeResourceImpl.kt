package com.tencent.devops.stream.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.pojo.Commit
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
        val setting = streamBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
        val token = oauthService.getAndCheckOauthToken(setting.enableUserId).accessToken
        return Result(
            streamScmService.getCommits(
                token = token,
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
}
