package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageCategoryResource
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.CategoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageCategoryResourceImpl @Autowired constructor(private val categoryService: CategoryService) :
    UserImageCategoryResource {

    override fun getAllImageCategorys(): Result<List<Category>?> {
        return categoryService.getAllCategory(StoreTypeEnum.IMAGE.type.toByte())
    }
}