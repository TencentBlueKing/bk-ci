package com.tencent.bk.codecc.defect.resources

import com.tencent.bk.codecc.defect.api.ServiceStatisticRestResource
import com.tencent.bk.codecc.defect.service.impl.CodeScoringServiceImpl
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStatisticRestResourceImpl @Autowired constructor(
    private val codeScoringServiceImpl: CodeScoringServiceImpl
) : ServiceStatisticRestResource {

    override fun getLintStatInfo(taskId: Long, toolName: String, buildId: String): Result<GrayTaskStatVO?> {
        return Result(codeScoringServiceImpl.getLintStatInfo(taskId, toolName, buildId))
    }
}
