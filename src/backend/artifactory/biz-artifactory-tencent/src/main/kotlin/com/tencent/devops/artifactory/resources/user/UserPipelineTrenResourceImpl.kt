package com.tencent.devops.artifactory.resources.user

import com.tencent.devops.artifactory.api.user.UserPipelineTrendResource
import com.tencent.devops.artifactory.pojo.TrendInfoDto
import com.tencent.devops.artifactory.service.ArtifactoryInfoService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@RestResource
class UserPipelineTrenResourceImpl @Autowired constructor(
    private val artifactoryInfoService: ArtifactoryInfoService
) : UserPipelineTrendResource {

    companion object {
        private val TIMEINTERVAL = TimeUnit.DAYS.toSeconds(90)
    }

    override fun constructApkAndIpaTrend(
        pipelineId: String,
        startTime: Long,
        endTime: Long,
        page: Int,
        pageSize: Int
    ): Result<TrendInfoDto> {
        if (pipelineId.isNullOrBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }

        val finishTime = checkTimeInterval(startTime, endTime)

        val result = artifactoryInfoService.queryArtifactoryInfo(pipelineId, startTime, finishTime)
        return Result(result)
    }

    // 查询时间区间最大为3个月
    private fun checkTimeInterval(startTime: Long, endTime: Long): Long {
        val finishTime: Long
        // 区间大于三个月需处理.endtime前推3个月.
        if (endTime - startTime > TIMEINTERVAL) {
            finishTime = endTime - TIMEINTERVAL
        } else {
            finishTime = endTime
        }
        return finishTime
    }
}
