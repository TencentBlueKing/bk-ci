package com.tencent.devops.process.api.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.process.pojo.third.tcls.TclsEnv
import com.tencent.devops.process.pojo.third.tcls.TclsType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.tools.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.Base64

@RestResource
class UserTclsResourceImpl @Autowired constructor(
    private val client: Client
) : UserTclsResource {

    override fun getEnvList(
        userId: String,
        projectId: String,
        tclsAppId: String?,
        mtclsApp: TclsType?,
        serviceId: String?,
        ticketId: String
    ): Result<List<TclsEnv>> {
        if (mtclsApp == TclsType.MTCLS) {
            if (StringUtils.isBlank(serviceId)) {
                throw ParamBlankException("Invalid serviceId")
            }
        } else {
            if (StringUtils.isBlank(tclsAppId)) {
                throw ParamBlankException("Invalid tclsAppId")
            }
        }

        return Result(getTcls(userId, projectId, tclsAppId, (mtclsApp == TclsType.MTCLS), serviceId, ticketId))
    }

    private fun getTcls(userId: String, projectId: String, tclsAppId: String?, isMtclsApp: Boolean, serviceId: String?, ticketId: String): List<TclsEnv> {
        val ticketsMap = getCredential(projectId, ticketId, CredentialType.USERNAME_PASSWORD)
        val account = ticketsMap["v1"].toString()
        val password = ticketsMap["v2"].toString()

        val url = if (isMtclsApp) "http://open.oa.com/component/compapi/mtcls/get_env_list/" else "http://open.oa.com/component/compapi/tcls/get_env_list/"
        val requestBody = if (isMtclsApp) {
            ObjectMapper().writeValueAsString(mapOf(
                    "app_code" to appCode,
                    "app_secret" to appSecret,
                    "operator" to userId,
                    "tcls_app_id" to serviceId,
                    "app_account" to account,
                    "password" to password
            ))
        } else {
            ObjectMapper().writeValueAsString(mapOf(
                    "app_code" to appCode,
                    "app_secret" to appSecret,
                    "operator" to userId,
                    "tcls_app_id" to tclsAppId,
                    "app_account" to account,
                    "password" to password
            ))
        }

        logger.info("POST url: $url")
        logger.info("requestBody: $requestBody")

//        val okHttpClient = okhttp3.OkHttpClient.Builder()
//                .connectTimeout(5L, TimeUnit.SECONDS)
//                .readTimeout(60L, TimeUnit.SECONDS)
//                .writeTimeout(60L, TimeUnit.SECONDS)
//                .build()
        val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody)).build()
//        val call = okHttpClient.newCall(request)
        OkhttpUtils.doHttp(request).use { response ->
            try {
//            val response = call.execute()
                val responseBody = response.body()?.string()
                logger.info("responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                if (responseData["result"] == false) {
                    val msg = responseData["message"]
                    logger.error("get env list failed: $msg")
                    throw OperationException("获取 TCLS 环境失败，请检查用户名密码是否正确，错误信息：$msg")
                }

                val dataList = responseData["data"] as List<Map<String, Any>>
                return dataList.map {
                    TclsEnv(it["env_id"].toString(), it["env_name"] as String)
                }.filterNot {
                    it.envId.toInt() in intArrayOf(
                        1,
                        2,
                        6
                    )
                } // 1,2,6是TCLS的正式环境，不能用，参考：http://tapd.oa.com/TCLS/markdown_wikis/#1010027201006683087
            } catch (e: Exception) {
                logger.error("get env list failed", e)
                throw OperationException("获取 TCLS 环境失败，请检查用户名密码是否正确")
            }
        }
    }

    private fun getCredential(projectId: String, credentialId: String, type: CredentialType): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(projectId, credentialId,
                encoder.encodeToString(pair.publicKey))
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            logger.error("Fail to get the credential($credentialId) of project($projectId) because of ${credentialResult.message}")
            throw OperationException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val credential = credentialResult.data!!
        if (type != credential.credentialType) {
            logger.error("CredentialId is invalid, expect:${type.name}, but real:${credential.credentialType.name}")
            throw ParamBlankException("Fail to get the credential($credentialId) of project($projectId)")
        }

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey))
        ticketMap["v1"] = v1

        if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            val v2 = String(DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v2"] = v2
        }

        if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            val v3 = String(DHUtil.decrypt(
                    decoder.decode(credential.v3),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v3"] = v3
        }

        if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            val v4 = String(DHUtil.decrypt(
                    decoder.decode(credential.v4),
                    decoder.decode(credential.publicKey),
                    pair.privateKey))
            ticketMap["v4"] = v4
        }

        return ticketMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserTclsResourceImpl::class.java)
        private val JSON = MediaType.parse("application/json;charset=utf-8")
        private const val appCode = "bkci"
        private const val appSecret = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
    }
}
