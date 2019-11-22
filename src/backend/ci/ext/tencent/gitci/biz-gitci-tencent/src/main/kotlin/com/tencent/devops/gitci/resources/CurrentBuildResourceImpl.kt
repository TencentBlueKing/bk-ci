package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.CurrentBuildResource
import com.tencent.devops.gitci.pojo.GitCIModelDetail
import com.tencent.devops.gitci.service.CurrentBuildService
import com.tencent.devops.gitci.service.GitProjectConfService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class CurrentBuildResourceImpl @Autowired constructor(
    private val currentBuildService: CurrentBuildService,
    private val gitProjectConfService: GitProjectConfService
) : CurrentBuildResource {
    override fun getLatestBuildDetail(userId: String, gitProjectId: Long, buildId: String?): Result<GitCIModelDetail?> {
        checkParam(userId)
        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
        return if (buildId.isNullOrBlank()) {
            Result(currentBuildService.getLatestBuildDetail(userId, gitProjectId))
        } else {
            Result(currentBuildService.getBuildDetail(userId, gitProjectId, buildId!!))
        }
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
