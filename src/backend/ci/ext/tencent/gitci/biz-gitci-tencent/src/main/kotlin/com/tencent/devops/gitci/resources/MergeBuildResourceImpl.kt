package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.MergeBuildResource
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.service.GitProjectConfService
import com.tencent.devops.gitci.service.MergeBuildService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class MergeBuildResourceImpl @Autowired constructor(
    private val mergeBuildService: MergeBuildService,
    private val gitProjectConfService: GitProjectConfService
) : MergeBuildResource {

    override fun getMergeBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Result<BuildHistoryPage<GitCIBuildHistory>> {
        checkParam(userId)
        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
        return Result(mergeBuildService.getMergeBuildList(userId, gitProjectId, page, pageSize))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
