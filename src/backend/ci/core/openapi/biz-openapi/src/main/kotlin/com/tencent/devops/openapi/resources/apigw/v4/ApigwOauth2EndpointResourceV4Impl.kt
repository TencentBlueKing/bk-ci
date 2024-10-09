package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.oauth2.Oauth2ServiceEndpointResource
import com.tencent.devops.auth.pojo.Oauth2AccessTokenRequest
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwOauth2EndpointResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwOauth2EndpointResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwOauth2EndpointResourceV4 {

    override fun getAccessToken(
        appCode: String?,
        apigwType: String?,
        clientId: String,
        clientSecret: String,
        accessTokenRequest: Oauth2AccessTokenRequest
    ): Result<Oauth2AccessTokenVo> {
        logger.info("OPENAPI_OAUTH2_ACCESS_TOKEN_V4|$appCode|$clientId")
        return try {
            client.get(Oauth2ServiceEndpointResource::class).getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                accessTokenRequest = accessTokenRequest
            )
        } catch (ex: ErrorCodeException) {
            Result(status = ex.errorCode.toInt(), message = ex.defaultMessage)
        } catch (rex: RemoteServiceException) {
            Result(status = rex.errorCode ?: REMOTE_EXCEPTION_CODE, message = rex.errorMessage)
        } catch (ignore: Exception) {
            throw ignore
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwOauth2EndpointResourceV4Impl::class.java)
        private const val REMOTE_EXCEPTION_CODE = 500
    }
}
