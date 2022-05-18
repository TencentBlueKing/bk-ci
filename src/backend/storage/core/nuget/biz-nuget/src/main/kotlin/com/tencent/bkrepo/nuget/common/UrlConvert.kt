package com.tencent.bkrepo.nuget.common

@FunctionalInterface
interface UrlConvert {
    fun convert(v2BaseUrl: String, v3BaseUrl: String): String
}
