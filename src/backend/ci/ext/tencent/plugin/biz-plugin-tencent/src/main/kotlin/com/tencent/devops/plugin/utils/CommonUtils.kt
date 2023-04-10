/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_ETH1_NETWORK_CARD_IP_EMPTY
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_FAILED_GET_NETWORK_CARD
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.constant.PluginMessageCode.GET_SIGNATURE_ERROR
import com.tencent.devops.plugin.constant.PluginMessageCode.URL_CODING_ERROR
import com.tencent.devops.plugin.constant.PluginMessageCode.WETEST_FAILED_GET
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import okhttp3.Request
import org.apache.commons.lang3.StringUtils
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URLEncoder
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)
    private val getKeyHost = "http://wetest.apigw.o.oa.com/prod/api"
    private val GET_API_KEY = "/v3/get_api_key"
    private val HMAC_SHA1 = "HmacSHA1"
    private val METHOD_GET = "GET"
//    private val okclient = OkhttpUtils.okHttpClient
    // esb校验
    private val APP_CODE = "bkci"
    private val APP_SECRET = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
    // wetest校验,通过用户名获取凭证所用
    private val APP_ID = 30005
    private val APP_KEY = "vnPcswYIlxk5SZZkYG0R"

    fun getInnerIP(): String {
        val ipMap = getMachineIP()
        var innerIp = ipMap["eth1"]
        if (StringUtils.isBlank(innerIp)) {
            logger.error(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_ETH1_NETWORK_CARD_IP_EMPTY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                )
            )
            innerIp = ipMap["eth0"]
        }
        if (StringUtils.isBlank(innerIp)) {
            val ipSet = ipMap.entries
            for ((_, value) in ipSet) {
                innerIp = value
                if (!StringUtils.isBlank(innerIp)) {
                    break
                }
            }
        }

        return if (StringUtils.isBlank(innerIp) || null == innerIp) "" else innerIp
    }

    fun getMachineIP(): Map<String, String> {
        logger.info("#####################Start getMachineIP")
        val allIp = HashMap<String, String>()

        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() // 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                logger.error("#####################getMachineIP Can not get NetworkInterfaces")
            } else {
                while (allNetInterfaces.hasMoreElements()) { // 循环网卡获取网卡的IP地址
                    val netInterface = allNetInterfaces.nextElement()
                    val netInterfaceName = netInterface.name
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equals(netInterfaceName, ignoreCase = true)) { // 过滤掉127.0.0.1的IP
                        logger.info(MessageUtil.getMessageByLocale(
                            messageCode = BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY,
                            language = I18nUtil.getDefaultLocaleLanguage()
                        ))
                    } else {
                        val addresses = netInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val ip = addresses.nextElement() as InetAddress
                            if (ip is Inet4Address && !ip.isLoopbackAddress) {
                                val machineIp = ip.hostAddress
                                logger.info("###############netInterfaceName=$netInterfaceName The Macheine IP=$machineIp")
                                allIp[netInterfaceName] = machineIp
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(MessageUtil.getMessageByLocale(
                messageCode = BK_FAILED_GET_NETWORK_CARD,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), e)
        }

        return allIp
    }

    fun getCredential(client: Client, projectId: String, credentialId: String, type: CredentialType): MutableMap<String, String> {
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

    fun getCredential(userId: String): Pair<String/*secretid*/, String/*secretkey*/> {
        val params = TreeMap<String, Any>(Comparator<String> { o1, o2 -> o1.compareTo(o2) })
        params["appid"] = APP_ID
        params["rtx"] = userId
        params["t"] = System.currentTimeMillis() / 1000

        val sb = StringBuilder()
        sb.append(GET_API_KEY + "?")
        var notfirst = false
        for ((key, value) in params) {
            if (notfirst) {
                sb.append("&")
            } else {
                notfirst = true
            }
            sb.append(String.format("%s=%s", key, value.toString()))
        }
        val signature = getKeySignature(METHOD_GET, GET_API_KEY, params, APP_KEY)
        val url = String.format("%s%s&sign=%s", getKeyHost, sb.toString(), signature)
        logger.info("wetest getApiKey request: $url")

        val header = TreeMap<String, Any>()
        header["bk_app_code"] = APP_CODE
        header["bk_app_secret"] = APP_SECRET
        val headerStr = ObjectMapper().writeValueAsString(header)

        val request = Request.Builder()
                .url(url)
                .addHeader("X-BKAPI-AUTHORIZATION", headerStr)
                .build()
        val response = this.doRequest(request)
        logger.info("wetest getApiKey response: $response")

        val ret = response.optInt("ret")
        if (ret != 0) {
            val msg = response.optString("msg")
            logger.error("fail to get getApiKey from weTest, retCode: $ret, msg: $msg")
            throw OperationException(
                MessageUtil.getMessageByLocale(
                    messageCode = WETEST_FAILED_GET,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(ret.toString(), msg)
                )
            )
        }

        val secretId = response.getString("secretid")
        val secretKey = response.getString("secretkey")
        return Pair(secretId, secretKey)
    }

    fun getKeySignature(method: String, url_path: String, params: Map<String, Any>, appKey: String): String {
        return try {
            val mac = Mac.getInstance(HMAC_SHA1)
            val secret = appKey + "&"
            val secretKey = SecretKeySpec(secret.toByteArray(charset("UTF-8")), mac.algorithm)
            mac.init(secretKey)
            val mk = makeSource(method, url_path, params)
            val hash = mac.doFinal(mk.toByteArray(charset("UTF-8")))
            encodeUrl(String(Base64Coder.encode(hash)))
        } catch (e: NoSuchAlgorithmException) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = GET_SIGNATURE_ERROR
                ) + "$e"
            )
        }
    }

    private fun makeSource(method: String, url_path: String, params: Map<String, Any>): String {
        val keys = params.keys.toTypedArray()
        Arrays.sort(keys)
        val buffer = StringBuilder(128)
        buffer.append(method.toUpperCase()).append("&").append(encodeUrl(url_path)).append("&")
        val buffer2 = StringBuilder()
        for (i in keys.indices) {
            buffer2.append(keys[i]).append("=").append(params[keys[i]])
            if (i != keys.size - 1) {
                buffer2.append("&")
            }
        }
        buffer.append(encodeUrl(buffer2.toString()))
        return buffer.toString()
    }

    private fun encodeUrl(input: String): String {
        try {
            return URLEncoder.encode(input, "UTF-8").replace("+", "%20").replace("*", "%2A")
        } catch (e: UnsupportedEncodingException) {
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = URL_CODING_ERROR
                ) + "$e"
            )
        }
    }

    private fun doRequest(request: Request): JSONObject {
        val errRet = JSONObject()
        errRet.put("ret", -1)
        try {
            OkhttpUtils.doHttp(request).use { response ->
                //            val response = okclient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body!!.string()
                    logger.info("WeTest response: $responseStr")
                    return JSONObject(responseStr)
                } else {
                    errRet.put("msg", "http code:" + response.code)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            errRet.put("msg", "IO exception, network not ok")
        } catch (e: JSONException) {
            errRet.put("msg", "json parse error, ret is not json")
        }
        return errRet
    }
}
