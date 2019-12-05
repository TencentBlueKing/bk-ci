package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageLabelResource
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.LabelService
import com.tencent.devops.store.service.image.ImageLabelService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageLabelResourceImpl @Autowired constructor(
    private val labelService: LabelService,
    private val imageLabelService: ImageLabelService
) :
    UserImageLabelResource {

    override fun getAllImageLabels(): Result<List<Label>?> {
        return labelService.getAllLabel(StoreTypeEnum.IMAGE.type.toByte())
    }

    override fun getImageLabelsByImageId(imageId: String): Result<List<Label>?> {
        return imageLabelService.getLabelsByImageId(imageId)
    }
}