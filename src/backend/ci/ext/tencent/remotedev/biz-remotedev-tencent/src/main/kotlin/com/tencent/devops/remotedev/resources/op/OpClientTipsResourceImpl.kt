package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.remotedev.api.op.OpClientTipsResource
import com.tencent.devops.remotedev.pojo.ClientTipsInfo
import com.tencent.devops.remotedev.service.ClientTipsService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpClientTipsResourceImpl @Autowired constructor(
    private val clientTipsService: ClientTipsService
) : OpClientTipsResource {
    override fun fetch(): Result<List<ClientTipsInfo>> {
        return Result(clientTipsService.fetchAll())
    }

    override fun createOrUpdate(id: Long?, data: ClientTipsInfo): Result<Boolean> {
        clientTipsService.createOrUpdateTips(id, data)
        return Result(true)
    }

    override fun deleteTips(ids: Set<Long>): Result<Boolean> {
        clientTipsService.deleteTips(ids)
        return Result(true)
    }
}