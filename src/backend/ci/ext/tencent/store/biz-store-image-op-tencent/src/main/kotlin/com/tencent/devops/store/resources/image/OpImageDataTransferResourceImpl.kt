package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.OpImageDataTransferResource
import com.tencent.devops.store.service.image.OpImageDataTransferService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpImageDataTransferResourceImpl @Autowired constructor(
    private val opImageDataTransferService: OpImageDataTransferService
) : OpImageDataTransferResource {
    override fun batchRecheckByProject(userId: String, projectCode: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.batchRecheckByProject(
                userId = userId,
                projectCode = projectCode,
                interfaceName = "/op/datatransfer/image/batchRecheckByProject,put"
            )
        )
    }

    override fun batchRecheckAll(userId: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.batchRecheckAll(
                userId = userId,
                interfaceName = "/op/datatransfer/image/batchRecheckAll,put"
            )
        )
    }

    override fun initClassifyAndCategory(
        userId: String,
        classifyCode: String?,
        classifyName: String?,
        categoryCode: String?,
        categoryName: String?
    ): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.initClassifyAndCategory(
                userId = userId,
                classifyCode = classifyCode,
                classifyName = classifyName,
                categoryCode = categoryCode,
                categoryName = categoryName,
                interfaceName = "/op/datatransfer/image/initClassifyAndCategory,put"
            )
        )
    }

    override fun transferImage(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        categoryCode: String?
    ): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.transferImage(
                userId = userId,
                projectCode = projectCode,
                classifyCode = classifyCode,
                categoryCode = categoryCode,
                interfaceName = "/op/datatransfer/image/transferImage,put"
            )
        )
    }
}