package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.leaf.common.Status
import com.tencent.devops.leaf.service.SegmentService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.pojo.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAllocIdResourceImpl @Autowired constructor(
    private val segmentService: SegmentService
) : ServiceAllocIdResource {

    override fun generateSegmentId(bizTag: String): Result<Long?> {
        val result = segmentService.getId(bizTag)
        if (result.status != Status.SUCCESS) {
            return Result(null)
        }
        return Result(result.id)
    }

    override fun batchGenerateSegmentId(bizTag: String, number: Int): Result<List<Long?>> {
        val list = mutableListOf<Long?>()
        for (i in 1..number) {
            val result = segmentService.getId(bizTag)
            if (result.status != Status.SUCCESS) {
                list.add(null)
                logger.warn("generate segment id failed , i:$i , bizTag:$bizTag , number:$number")
            } else {
                list.add(result.id)
            }
        }
        return Result(list)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceAllocIdResourceImpl::class.java)
    }
}
