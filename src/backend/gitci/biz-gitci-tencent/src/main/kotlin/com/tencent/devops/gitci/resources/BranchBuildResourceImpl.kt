package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.BranchBuildResource
import com.tencent.devops.gitci.pojo.BranchBuildHistory
import com.tencent.devops.gitci.service.BranchBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BranchBuildResourceImpl @Autowired constructor(private val branchBuildService: BranchBuildService) : BranchBuildResource {

    override fun getBranchBuildList(userId: String, gitProjectId: Long, defaultBranch: String?): Result<List<BranchBuildHistory>> {
        checkParam(userId)
        return Result(branchBuildService.getBranchBuildList(userId, gitProjectId, defaultBranch))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
