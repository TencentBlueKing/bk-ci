package com.tencent.devops.monitoring.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.monitoring.api.service.SlaMonitorResource
import com.tencent.devops.monitoring.pojo.SlaCodeccResponseData
import com.tencent.devops.monitoring.services.SlaMonitorService
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class SlaMonitorResourceImpl @Autowired constructor(
    private val slaMonitorService: SlaMonitorService
) : SlaMonitorResource {
    override fun codeccQuery(bgId: String, startTime: Long, endTime: Long): Result<SlaCodeccResponseData> {
        if (startTime > System.currentTimeMillis() || startTime > endTime) {
            logger.error("wrong timestamp , startTime:$startTime , endTime:$endTime")
            return Result(-1, "非法时间戳范围")
        }

        if (!NumberUtils.isParsable(bgId)) {
            logger.error("wrong bgId , bgId:$bgId")
            return Result(-2, "非法事业群ID")
        }

        return Result(slaMonitorService.codeccQuery(bgId, startTime, endTime))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlaMonitorResourceImpl::class.java)
    }
}
