package com.tencent.bk.codecc.openapi.filter

import com.tencent.bk.codecc.openapi.config.ApiGatewayAuthProperties
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.bk.codecc.openapi.utils.ApiGatewayPubFile
import io.jsonwebtoken.Jwts
import net.sf.json.JSONObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class ApiFilter : ContainerRequestFilter {
    fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        val enabled = ApiGatewayAuthProperties.properties?.enabled ?: ""
        if(!StringUtils.hasLength(enabled) || enabled == "false"){
            return true
        }
        val bkApiJwt = requestContext.getHeaderString("X-Bkapi-JWT")
        val apigwtType = requestContext.getHeaderString("X-DEVOPS-APIGW-TYPE")
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.request}")
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request bkapi jwt is empty.")
                    .build())
            return false
        }

        val uriPath = requestContext.uriInfo.requestUri.path
        val apiType = if (uriPath.startsWith("/api/apigw-app")) "apigw-app" else "apigw-user"

        logger.info("Get the bkApiJwt header, X-Bkapi-JWT：$bkApiJwt")
        val jwt = parseJwt(bkApiJwt, apigwtType)
        logger.info("Get the parse bkApiJwt($jwt)")

        // 验证应用身份信息
        if (jwt.has("app")) {
            val app = jwt.getJSONObject("app")
            // 应用身份登录
            if (app.has("app_code")) {
                val appCode = app.getString("app_code")
                val verified = app.get("verified") as Boolean
                if (appCode.isNullOrEmpty() || !verified) {
                    return false
                } else {
                    if (!appCode.isNullOrBlank()) {
                        // 将appCode头部置空
                        requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                        if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                            requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, appCode)
                        } else {
                            requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, appCode)
                        }
                    }
                }
            }
        }
        // 在验证应用身份信息
        if (jwt.has("user")) {
            // 先做app的验证再做
            val user = jwt.getJSONObject("user")
            // 用户身份登录
            if (user.has("username")) {
                val username = user.getString("username")
                val verified = user.get("verified") as Boolean
                // 名字为空或者没有通过认证的时候，直接失败
                if (username.isNotBlank() && verified) {
                    // 将头部置空
                    requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, null)
                    if (requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID] != null) {
                        requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.set(0, username)
                    } else {
                        requestContext.headers.add(AUTH_HEADER_DEVOPS_USER_ID, username)
                    }
                } else if (apiType == "apigw-user") {
                    requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                            .entity("Request don't has user's access_token.")
                            .build())
                    return false
                }
            }
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val valid = verifyJWT(requestContext)
        // 验证通过
        if (!valid) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("蓝盾Devops OpenAPI认证失败：用户或应用验证失败。")
                    .build()
            )
            return
        }
    }

    private fun parseJwt(bkApiJwt: String, apigwtType: String?): JSONObject {
        var reader: PemReader? = null
        try {
            val key = if (!apigwtType.isNullOrEmpty() && apigwtType == "outer") {
                SpringContextUtil.getBean(ApiGatewayPubFile::class.java).getPubOuter().toByteArray()
            } else {
                SpringContextUtil.getBean(ApiGatewayPubFile::class.java).getPubInner().toByteArray()
            }
            Security.addProvider(BouncyCastleProvider())
            val bais = ByteArrayInputStream(key)
            reader = PemReader(InputStreamReader(bais))
            val jwtParser = Jwts.parser().setSigningKey(reader.readPemObject().content)
            val parse = jwtParser.parse(bkApiJwt)
            logger.info("Get the parse body(${parse.body}) and header(${parse.header})")
            return JSONObject.fromObject(parse.body)
        } catch (e: Exception) {
            logger.error("Parse jwt failed.", e)
            throw e
        } finally {
            if (reader != null) {
                reader.close()
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiFilter::class.java)
    }
}