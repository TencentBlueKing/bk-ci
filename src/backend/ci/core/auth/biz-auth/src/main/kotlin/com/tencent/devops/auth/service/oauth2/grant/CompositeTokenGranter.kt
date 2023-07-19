package com.tencent.devops.auth.service.oauth2.grant

class CompositeTokenGranter constructor(
    private val tokenGranters: List<TokenGranter>
) : TokenGranter {
    override fun grant(grantType: String): String? {
        for (granter in tokenGranters) {
            val grant = granter.grant(grantType)
            if (grant != null) {
                return grant
            }
        }
        return null
    }
}
