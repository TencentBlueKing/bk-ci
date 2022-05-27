package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

data class RegistrationItem(
    /* The URL to the registration page */
    @JsonProperty("@id")
    val id: URI,
    /* The number of registration leaves in the page */
    val count: Int,
    // 当包的版本数过多时，可以不显示items列表，必须使用@id中指定的URL来获取有关各个包版本的元数据。
    // 作为一种优化，items数组有时会从page对象中排除。如果单个包ID的版本数非常大，
    // 那么对于只关心特定版本或小范围版本的客户机来说，注册索引文档的处理将是巨大和浪费的。
    // The array of registration leaves and their associate metadata
    val items: List<RegistrationPageItem>? = null,
    // The lowest SemVer 2.0.0 version in the page (inclusive)
    val lower: String,
    // 当展示items属性时parent属性才会出现
    // The URL to the registration index
    // The parent property will only appear if the registration page object has the items property.
    val parent: URI? = null,
    // 	The highest SemVer 2.0.0 version in the page (inclusive)
    val upper: String
)
