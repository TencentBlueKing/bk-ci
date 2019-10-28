package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.UserIdeAtomCategoryResource
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.CategoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserIdeAtomCategoryResourceImpl @Autowired constructor(private val categoryService: CategoryService) :
    UserIdeAtomCategoryResource {

    override fun getAllIdeAtomCategorys(): Result<List<Category>?> {
        return categoryService.getAllCategory(StoreTypeEnum.IDE_ATOM.type.toByte())
    }
}