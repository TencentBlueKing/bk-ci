package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.UserIdeAtomLabelResource
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.LabelService
import com.tencent.devops.store.service.ideatom.IdeAtomLabelService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserIdeAtomLabelResourceImpl @Autowired constructor(
    private val labelService: LabelService,
    private val atomLabelService: IdeAtomLabelService
) : UserIdeAtomLabelResource {

    override fun getAllAtomLabels(): Result<List<Label>?> {
        return labelService.getAllLabel(StoreTypeEnum.IDE_ATOM.type.toByte())
    }

    override fun getAtomLabelsByAtomId(atomId: String): Result<List<Label>?> {
        return atomLabelService.getLabelsByAtomId(atomId)
    }
}