package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.HistoryBuildResource
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.service.HistoryBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class HistoryBuildResourceImpl @Autowired constructor(private val historyBuildService: HistoryBuildService) : HistoryBuildResource {
    override fun getHistoryBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Result<BuildHistoryPage<GitCIBuildHistory>> {
        checkParam(userId)
        return Result(historyBuildService.getHistoryBuildList(userId, gitProjectId, page, pageSize))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
