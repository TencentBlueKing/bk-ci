package com.tencent.devops.project.api.pojo

data class ServiceItem (
    private val itemId: String,
    private val itemName: String,
    private val itemCode: String,
    private val parentId: String
)