package com.tencent.devops.store.pojo.trigger

import com.tencent.devops.common.web.utils.I18nUtil

data class TriggerGroupInfo(
    val name: String,
    val ownerStoreCode: String,
    val count: Int
) {
    constructor(ownerStoreCode: String, count: Int) : this(
        name = I18nUtil.getCodeLanMessage(ownerStoreCode),
        ownerStoreCode = ownerStoreCode,
        count = count
    )
}
