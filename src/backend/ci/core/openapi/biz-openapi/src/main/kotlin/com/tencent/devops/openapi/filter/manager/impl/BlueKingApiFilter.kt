package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_JWT_PARSE_FAIL
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.openapi.utils.ApiGatewayPubFile
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import io.jsonwebtoken.Jwts
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security
import javax.ws.rs.core.Response
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BlueKingApiFilter constructor(
    private val apiGatewayUtil: ApiGatewayUtil
) : ApiFilterManager {
    companion object {
        private val logger = LoggerFactory.getLogger(BlueKingApiFilter::class.java)
        private const val appCodeHeader = "app_code"
        private const val jwtHeader = "X-Bkapi-JWT"
    }

    @Value("\${api.blueKing.enable:#{null}}")
    private val apiFilterEnabled: Boolean? = false

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        if (!apiGatewayUtil.isAuth() || apiFilterEnabled != true) return false
        requestContext.needCheckPermissions = true
        return !requestContext.requestContext.getHeaderString(jwtHeader).isNullOrBlank()
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val bkApiJwt = requestContext.requestContext.getHeaderString(jwtHeader)
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.requestContext.request}")
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request bkapi jwt is empty.")
                    .build()
            )
            return ApiFilterFlowState.BREAK
        }

        val jwt = parseJwt(bkApiJwt)
        logger.debug("Get the bkApiJwt header|X-Bkapi-JWT={}|jwt={}", bkApiJwt, jwt)

        // 验证应用身份信息
        if (jwt.contains("app")) {
            val app = jwt["app"] as Map<String, Any>
            // 应用身份登录
            if (app.contains(appCodeHeader)) {
                val appCode = app[appCodeHeader]?.toString()
                val verified = app["verified"].toString().toBoolean()
                if (requestContext.apiType == ApiPathFilter.ApiType.APP && (appCode.isNullOrEmpty() || !verified)) {
                    requestContext.requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity("Devops OpenAPI Auth fail：app auth fail.")
                            .build()
                    )
                    return ApiFilterFlowState.BREAK
                } else {
                    if (!appCode.isNullOrBlank()) {
                        // 将appCode头部置空
                        requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                        if (requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                            requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, appCode)
                        } else {
                            requestContext.requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, appCode)
                        }
                    }
                }
            }
        }
        // 在验证应用身份信息
        if (jwt.contains("user")) {
            // 先做app的验证再做
            val user = jwt["user"] as Map<String, Any>
            // 用户身份登录
            if (user.contains("username")) {
                val username = user["username"]?.toString() ?: ""
                val verified = user["verified"].toString().toBoolean()
                // 名字为空或者没有通过认证的时候，直接失败
                if (username.isNotBlank() && verified) {
                    // 将user头部置空
                    requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, null)
                    if (requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID] != null) {
                        requestContext.requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, username)
                    } else {
                        requestContext.requestContext.headers.add(AUTH_HEADER_DEVOPS_USER_ID, username)
                    }
                } else if (requestContext.apiType == ApiPathFilter.ApiType.USER) {
                    requestContext.requestContext.abortWith(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity("Request don't has user's access_token.")
                            .build()
                    )
                    return ApiFilterFlowState.BREAK
                }
            }
        }
        return ApiFilterFlowState.BREAK
    }

    private fun parseJwt(bkApiJwt: String): Map<String, Any> {
        var reader: PEMParser? = null
        try {
            val key = SpringContextUtil.getBean(ApiGatewayPubFile::class.java).getPubOuter().toByteArray()

            Security.addProvider(BouncyCastleProvider())
            val bais = ByteArrayInputStream(key)
            reader = PEMParser(InputStreamReader(bais))
            val publicKeyInfo = reader.readObject() as SubjectPublicKeyInfo
            val publicKey = JcaPEMKeyConverter().getPublicKey(publicKeyInfo)
            val jwtParser = Jwts.parserBuilder().setSigningKey(publicKey).build()
            val parse = jwtParser.parse(bkApiJwt)
            logger.info("Get the parse body(${parse.body}) and header(${parse.header})")
            return JsonUtil.toMap(parse.body)
        } catch (ignored: Exception) {
            logger.error("BKSystemErrorMonitor| Parse jwt failed.", ignored)
            throw ErrorCodeException(
                errorCode = ERROR_OPENAPI_JWT_PARSE_FAIL,
                defaultMessage = "Parse jwt failed",
                params = arrayOf(bkApiJwt)
            )
        } finally {
            reader?.close()
        }
    }
}
