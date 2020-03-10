package com.tencent.devops.store.pojo

data class ExtensionJson(
    val serviceCode: String? = null,
    val itemList: List<ItemProps>? = emptyList()
)