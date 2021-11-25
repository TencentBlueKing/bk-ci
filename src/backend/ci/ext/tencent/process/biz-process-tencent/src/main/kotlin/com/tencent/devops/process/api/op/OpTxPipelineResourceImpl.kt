package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.dao.TxPipelineInfoDao
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTxPipelineResourceImpl @Autowired constructor(
    val pipelineInfoDao: TxPipelineInfoDao,
    val dslContext: DSLContext,
    val client: Client
) : OpTxPipelineResource {
    override fun updatePipelineCreator(pipelineId: String, creator: String): Result<Boolean> {
        // 校验用户是否为tx在职用户
        client.get(ServiceTxUserResource::class).get(creator)

        pipelineInfoDao.updateCreator(dslContext, pipelineId, creator)

        return Result(true)
    }
}
