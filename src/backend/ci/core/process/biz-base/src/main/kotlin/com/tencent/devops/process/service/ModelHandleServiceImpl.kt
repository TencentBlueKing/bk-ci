package com.tencent.devops.process.service

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.service.`var`.PublicVarService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ModelHandleServiceImpl @Autowired constructor(
    private val publicVarService: PublicVarService
) : ModelHandleService {

    override fun handleModelParams(
        projectId: String,
        model: Model,
        referId: String,
        referType: String,
        referVersion: Int
    ) {
        publicVarService.handleModelParams(
            projectId = projectId,
            model = model,
            referId = referId,
            referType = PublicVerGroupReferenceTypeEnum.valueOf(referType),
            referVersion = referVersion
        )
    }
}
