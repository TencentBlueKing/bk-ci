package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.ServiceLogResource
import com.tencent.devops.gitci.service.GitProjectConfService
import com.tencent.devops.gitci.service.LogService
import com.tencent.devops.log.model.pojo.QueryLogs
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class ServiceLogResourceImpl @Autowired constructor(
    private val logService: LogService,
    private val gitProjectConfService: GitProjectConfService
) : ServiceLogResource {
    override fun getInitLogs(gitProjectId: Long, buildId: String, isAnalysis: Boolean?, queryKeywords: String?, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getInitLogs(gitProjectId, buildId, isAnalysis, queryKeywords, tag, jobId, executeCount))
    }

    override fun getMoreLogs(gitProjectId: Long, buildId: String, num: Int?, fromStart: Boolean?, start: Long, end: Long, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getMoreLogs(gitProjectId, buildId, num, fromStart, start, end, tag, jobId, executeCount))
    }

    override fun getAfterLogs(gitProjectId: Long, buildId: String, start: Long, isAnalysis: Boolean?, queryKeywords: String?, tag: String?, jobId: String?, executeCount: Int?): Result<QueryLogs> {
        checkParam(buildId, gitProjectId)
        return Result(logService.getAfterLogs(gitProjectId, buildId, start, isAnalysis, queryKeywords, tag, jobId, executeCount))
    }

    override fun downloadLogs(gitProjectId: Long, buildId: String, tag: String?, jobId: String?, executeCount: Int?): Response {
        checkParam(buildId, gitProjectId)
        return logService.downloadLogs(gitProjectId, buildId, tag, jobId, executeCount)
    }

    private fun checkParam(buildId: String, gitProjectId: Long) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (!gitProjectConfService.isEnable(gitProjectId)) {
            throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，请联系蓝盾助手")
        }
    }
}