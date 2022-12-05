package com.tencent.bkrepo.nuget.pojo.v3.metadata.index

/**
 * reference: https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-index
 */
data class RegistrationIndex(
    // The number of registration pages in the index
    val count: Int,
    // The array of registration pages
    val items: List<RegistrationItem>
)
