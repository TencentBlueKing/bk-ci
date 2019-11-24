package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.BranchBuildResource
import com.tencent.devops.gitci.pojo.BranchBuildHistory
import com.tencent.devops.gitci.service.BranchBuildService
import com.tencent.devops.gitci.service.GitProjectConfService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BranchBuildResourceImpl @Autowired constructor(
    private val branchBuildService: BranchBuildService,
    private val gitProjectConfService: GitProjectConfService
) : BranchBuildResource {

    override fun getBranchBuildList(userId: String, gitProjectId: Long, defaultBranch: String?): Result<List<BranchBuildHistory>> {
        checkParam(userId)
        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
        return Result(branchBuildService.getBranchBuildList(userId, gitProjectId, defaultBranch))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
