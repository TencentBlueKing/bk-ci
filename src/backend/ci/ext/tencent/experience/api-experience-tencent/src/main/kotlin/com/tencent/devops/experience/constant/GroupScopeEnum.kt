package com.tencent.devops.experience.constant

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

/**
 * 体验范围
 */
enum class GroupScopeEnum(
    val id: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "groupScopeEnum", reusePrefixFlag = false)
    val mean: String
) {
    PUBLIC(0, "public"),//公开体验

    PRIVATE(1, "private"),//内部体验

    ;
}
