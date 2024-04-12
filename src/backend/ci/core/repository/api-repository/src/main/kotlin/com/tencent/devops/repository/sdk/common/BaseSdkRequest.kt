package com.tencent.devops.repository.sdk.common

abstract class BaseSdkRequest<T> : SdkRequest<T> {
    private val headerMap: MutableMap<String, String> = mutableMapOf()

    override fun getHeaderMap(): Map<String, String> {
        return headerMap
    }

    private val udfParams: MutableMap<String, String> = mutableMapOf()

    override fun getUdfParams(): Map<String, String> {
        return udfParams
    }

    fun putHeaderParam(key: String, value: String) {
        headerMap[key] = value
    }

    fun putUdfParam(key: String, value: String) {
        udfParams[key] = value
    }
}
