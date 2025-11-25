package com.tencent.devops.store.pojo.trigger

import com.tencent.devops.common.web.utils.I18nUtil

data class TriggerGroupInfo(
    val name: String,
    val type: String,
    val count: Int
) {
    constructor(type: String, count: Int) : this(
        name = I18nUtil.getCodeLanMessage(type),
        type = type,
        count = count
    )
}
