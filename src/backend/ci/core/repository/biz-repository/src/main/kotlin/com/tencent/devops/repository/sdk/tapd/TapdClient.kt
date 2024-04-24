package com.tencent.devops.repository.sdk.tapd

interface TapdClient {

    fun <T> execute(oauthToken: String, request: TapdRequest<T>): T

    fun <T> execute(request: TapdRequest<T>): T

    fun appInstallUrl(
        cb: String,
        state: String? = null,
        test: Int = 1,
        showInstalled: Int? = null
    ): String

    fun oauthUrl(
        redirectUri: String,
        scope: String,
        authBy: TapdAuthType = TapdAuthType.WORKSPACE,
        state: String
    ): String
}
