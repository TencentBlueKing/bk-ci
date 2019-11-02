package com.tencent.devops.store.resources.image.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.op.OpImageDataTransferResource
import com.tencent.devops.store.service.image.op.OpImageDataTransferService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpImageDataTransferResourceImpl @Autowired constructor(
    private val opImageDataTransferService: OpImageDataTransferService
) : OpImageDataTransferResource {
    override fun clearFinishedSet(userId: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.clearFinishedSet(
                userId = userId,
                interfaceName = "/op/datatransfer/image/clearFinishedSet,put"
            )
        )
    }

    override fun transferImage(userId: String, projectCode: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.transferImage(
                userId = userId,
                projectCode = projectCode,
                interfaceName = "/op/datatransfer/image/transferImage,put"
            )
        )
    }
}