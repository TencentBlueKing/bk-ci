package com.tencent.devops.store.pojo

data class ItemProps(
    val itemCode: String,
    val props: Map<String, Any>? = null
)