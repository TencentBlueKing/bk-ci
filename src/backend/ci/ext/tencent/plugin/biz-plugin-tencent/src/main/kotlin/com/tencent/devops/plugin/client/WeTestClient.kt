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

package com.tencent.devops.plugin.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.plugin.pojo.wetest.WetestReportResponse
import com.tencent.devops.plugin.utils.CommonUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import sun.misc.BASE64Encoder
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.Comparator
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class WeTestClient constructor(private val secretId: String, private val secretKey: String) {

    companion object {
        private val wetestHost = "http://api.wetest.oa.com"
        private val host = "http://wetest.apigw.o.oa.com/prod"
        private val expire = 600L
        private val METHOD_POST = "POST"
        private val METHOD_GET = "GET"
        private val API_PRIVATE_TEST_INFO = "/cloudapi/api_v4/private_test_info?"
        private val API_PRIVATE_TEST_IMAGE_LOG = "/cloudapi/api_v4/private_test_image_log?"
        private val API_PRIVATE_TEST_DEVICE_IMAGE_LOG = "/cloudapi/api_v4/private_test_device_image_log?"
        private val API_PRIVATE_TEST_ERROR_PERF = "/cloudapi/api_v4/private_test_perf_error?"
        private val API_PRIVATE_TEST_DEVICE_ERROR_PERF = "/cloudapi/api_v4/private_test_device_perf_error?"
        private val API_PRIVATE_START_URL_TEST = "/cloudapi/api_v4/start_url_test?"
        private val API_PRIVATE_MY_CLOUD = "/cloudapi/api_v4/private_my_cloud?"
        private val API_PRIVATE_CLOUD_DEVICES = "/cloudapi/api_v4/private_cloud_devices?"
        private val API_FILE_UPLOAD = "/cloudapi/api_v4/fileupload?"
        private val API_AUTO_TEST = "/cloudapi/api_v4/private_user_autotest?"
        private val API_TEST_STATUS = "/cloudapi/api_v4/private_test_status?"
        private val API_TEST_TYPE = "/cloudapi/api_v4/private_testtypes?"
        private val API_LOG_CONTENT = "/cloudapi/api_v4/private_logcontent?"
        private val API_GET_SESSION = "/oaapi/open_api/login?"
        private val API_GET_GROUP = "/v3/get_group"
        private val API_GET_ENGINEDATA = "/cloudapi/api_v4/private_enginedata?"
        private val HMAC_SHA256 = "HmacSHA256"
//        private val okclient = OkhttpUtils.okHttpClient
        private val logger = LoggerFactory.getLogger(WeTestClient::class.java)

        // esb校验
        private val APP_CODE = "bkci"
        private val APP_SECRET = "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<"
        // wetest校验签名使用
        private val APP_ID = 30005
        private val APP_KEY = "vnPcswYIlxk5SZZkYG0R"
    }

    private fun getSignature(data: String): String? {
        return try {
            val sigingKey = SecretKeySpec(this.secretKey.toByteArray(), HMAC_SHA256)
            val mac = Mac.getInstance(HMAC_SHA256)
            mac.init(sigingKey)
            val rawHmac = mac.doFinal(data.toByteArray())
            val signature = BASE64Encoder().encode(rawHmac)
            URLEncoder.encode(signature, "UTF-8")
        } catch (e: Exception) {
            null
        }
    }

    private fun getAuthRequestUrl(method: String, uri: String, queryParams: Map<String, Any>?): String {
        val nonce = (Math.random() * 99999).toInt()
        val timestamp = System.currentTimeMillis() / 1000
        val params = TreeMap<String, Any>(Comparator<String> { o1, o2 -> o1.compareTo(o2) })
        params["timestamp"] = timestamp
        params["nonce"] = nonce
        params["signaturemethod"] = "HmacSHA256"
        params["expire"] = timestamp + expire
        params["secretid"] = this.secretId

        if (queryParams != null) {
            for ((key, value) in queryParams) {
                params[key] = value
            }
        }
        val sb = StringBuilder()
        sb.append(uri)
        var notfirst = false
        for ((key, value) in params) {
            if (notfirst) {
                sb.append("&")
            } else {
                notfirst = true
            }
            sb.append(String.format("%s=%s", key, value.toString()))
        }
        val sourcestr = sb.toString()
        val signature = getSignature(method + sourcestr)
        if (uri.equals(API_FILE_UPLOAD)) {
            // 上传文件接口不能走esb，需要直接走wetest
            return String.format("%s%s&signature=%s", wetestHost, sourcestr, signature)
        }
        return String.format("%s%s&signature=%s", host, sourcestr, signature)
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

    private fun doGetRequest(uri: String, params: Map<String, Any>): JSONObject {
        val url = this.getAuthRequestUrl(METHOD_GET, uri, params)
        logger.info("wetest get request: $url")

        val header = TreeMap<String, Any>()
        val bkNonce = (Math.random() * 99999).toInt()
        val bkTimestamp = System.currentTimeMillis() / 1000
        val bkSignature = "YtNY76EfatYf2Rxl9Eo5"
        header["bk_nonce"] = bkNonce
        header["bk_timestamp"] = bkTimestamp
        header["bk_signature"] = bkSignature
        header["bk_app_code"] = APP_CODE
        header["bk_app_secret"] = APP_SECRET
        header["app_code"] = APP_CODE
        header["app_secret"] = APP_SECRET
        val headerStr = ObjectMapper().writeValueAsString(header)

        val request = Request.Builder()
            .url(url)
            .addHeader("X-BKAPI-AUTHORIZATION", headerStr)
            .build()
        val result = this.doRequest(request)
        logger.info("wetest get request result: $result")
        return result
    }

    private fun doPostRequest(uri: String, jsonbody: JSONObject, queryParams: Map<String, Any>?): JSONObject {
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonbody.toString())
        val url = this.getAuthRequestUrl(METHOD_POST, uri, queryParams)
        logger.info("wetest post url: $url")
        logger.info("wetest post body: $jsonbody")

        val header = TreeMap<String, Any>()
        val bkNonce = (Math.random() * 99999).toInt()
        val bkTimestamp = System.currentTimeMillis() / 1000
        val bkSignature = "YtNY76EfatYf2Rxl9Eo5"
        header["bk_nonce"] = bkNonce
        header["bk_timestamp"] = bkTimestamp
        header["bk_signature"] = bkSignature
        header["bk_app_code"] = APP_CODE
        header["bk_app_secret"] = APP_SECRET
        header["app_code"] = APP_CODE
        header["app_secret"] = APP_SECRET
        val headerStr = ObjectMapper().writeValueAsString(header)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-BKAPI-AUTHORIZATION", headerStr)
            .addHeader("Content-Type", "application/json")
            .build()
        val result = this.doRequest(request)
        logger.info("wetest post request result: $result")
        return result
    }

    /***
     * 拉取个人私有云配置
     * @return
     */
    fun getCloudDevices(cloudIds: String, online: String, free: String): JSONObject {
        val params = HashMap<String, Any>()
        params["cloudids"] = cloudIds
        params["online"] = online
        params["free"] = free
        return this.doGetRequest(API_PRIVATE_CLOUD_DEVICES, params)
    }

    /***
     * 拉取个人私有云配置
     * @return
     */
    fun getMyCloud(): JSONObject {
        val params = HashMap<String, Any>()
        return this.doGetRequest(API_PRIVATE_MY_CLOUD, params)
    }

    /***
     * 测试设备列表和测试整体情况返回
     * @param testid
     * @return
     */
    fun getTestInfo(testid: String): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testid
        return this.doGetRequest(API_PRIVATE_TEST_INFO, params)
    }

    /***
     * 拉取整个测试的截图和日志
     * @param testid 测试ID
     * @param needimage 不需要截图
     * @param needlog 不需要日志
     * @return
     */
    fun getTestImageLog(testid: String, needimage: Boolean, needlog: Boolean): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testid
        if (!needimage) {
            params["needimage"] = 0
        }
        if (!needlog) {
            params["needlog"] = 0
        }
        return this.doGetRequest(API_PRIVATE_TEST_IMAGE_LOG, params)
    }

    /***
     * 拉取一个测试中单个设备的截图和日志
     * @param testid
     * @param deviceid
     * @param needimage
     * @param needlog
     * @return
     */
    fun getTestDeviceImageLog(testid: String, deviceid: String, needimage: Boolean, needlog: Boolean): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testid
        params["deviceid"] = deviceid
        if (!needimage) {
            params["needimage"] = 0
        }
        if (!needlog) {
            params["needlog"] = 0
        }
        return this.doGetRequest(API_PRIVATE_TEST_DEVICE_IMAGE_LOG, params)
    }

    /***
     * 拉取整个测试的性能数据和错误日志
     * @param testid
     * @param neederror
     * @param needperf
     * @return
     */
    fun getTestPerfError(testid: String, neederror: Boolean, needperf: Boolean): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testid
        if (!needperf) {
            params["needperf"] = 0
        }
        if (!neederror) {
            params["neederror"] = 0
        }
        return this.doGetRequest(API_PRIVATE_TEST_ERROR_PERF, params)
    }

    /***
     * 根据测试ID拉取测试相关的信息
     * @param testid
     * @param neederror
     * @param needperf
     * @return
     */
    fun getLogContent(testId: String, deviceId: String, level: String, startLine: Int, lineCnt: Int): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testId
        params["deviceid"] = deviceId
        params["level"] = level
        params["startline"] = startLine
        params["linecnt"] = lineCnt

        return this.doGetRequest(API_LOG_CONTENT, params)
    }

    /***
     * 拉取测试中单个设备的性能和错误日志
     * @param testid
     * @param deviceid
     * @param needperf
     * @param neederror
     * @return
     */
    fun getTestDevicePerfError(testid: String, deviceid: String, neederror: Boolean, needperf: Boolean): JSONObject {
        val params = HashMap<String, Any>()
        params["testid"] = testid
        params["deviceid"] = deviceid
        if (!needperf) {
            params["needperf"] = 0
        }
        if (!neederror) {
            params["neederror"] = 0
        }
        return this.doGetRequest(API_PRIVATE_TEST_DEVICE_ERROR_PERF, params)
    }

    /***
     * 上传资源
     * @param type
     * @return
     */
    fun uploadRes(type: String, file: File): JSONObject {
        val params = mapOf("type" to type)
        logger.info("upload res params: $params")
        val fileBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .addFormDataPart("type", type)
            .build()
        val url = getAuthRequestUrl(METHOD_POST, API_FILE_UPLOAD, params)
        logger.info("wetest上传包url ：（$url）")
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        return this.doRequest(request)
    }

    /***
     * 提交测试
     * @return
     */
    fun autoTest(params: WetestAutoTestRequest): JSONObject {
        logger.info("autoTest params: $params")
        return this.doPostRequest(API_AUTO_TEST, JSONObject(params), null)
    }

    /***
     * 测试进度查询
     * @return
     */
    fun testStatus(testid: String): JSONObject {
        return this.doGetRequest(API_TEST_STATUS, mapOf("testid" to testid))
    }

    /***
     * 测试进度查询
     * @return
     */
    fun testTypes(): JSONObject {
        return this.doGetRequest(API_TEST_TYPE, HashMap())
    }

    /**
     * 获取wetest登陆的session
     */
    fun getSession(userId: String): JSONObject {
        logger.info("getSession params: userId = $userId")
        val params = mapOf("rtx" to userId)
        return this.doPostRequest(API_GET_SESSION, JSONObject(), params)
    }

    /**
     * 获取wetest登陆的session
     */
    fun getEmailData(testid: String): JSONObject {
        return this.doGetRequest(API_GET_ENGINEDATA, mapOf("testid" to testid))
    }

    fun getWetestGroup(): WetestReportResponse {
        val t = System.currentTimeMillis() / 1000
        val params = mapOf(
            "appid" to APP_ID,
            "secretid" to secretId,
            "t" to t
        )

        val sb = StringBuilder()
        sb.append(API_GET_GROUP + "?")
        var notfirst = false
        for ((key, value) in params) {
            if (notfirst) {
                sb.append("&")
            } else {
                notfirst = true
            }
            sb.append(String.format("%s=%s", key, value.toString()))
        }
        val signature = CommonUtils.getKeySignature(METHOD_GET, API_GET_GROUP, params, APP_KEY)
        val url = String.format("%s%s&sign=%s", host + "/api", sb.toString(), signature)
        logger.info("wetest getGroup request: $url")

        val header = TreeMap<String, Any>()
        header["bk_app_code"] = APP_CODE
        header["bk_app_secret"] = APP_SECRET
        val headerStr = ObjectMapper().writeValueAsString(header)

        val request = Request.Builder()
            .url(url)
            .addHeader("X-BKAPI-AUTHORIZATION", headerStr)
            .build()
        val response = this.doRequest(request)
        logger.info("wetest getGroup response: $response")

        val ret = response.optInt("ret")
        if (ret != 0 || response.optString("projects") == null) {
            val msg = response.optString("msg")
            logger.error("fail to get getGroup from weTest, retCode: $ret, msg: $msg")
            throw OperationException("WeTest获取group失败，返回码: $ret, 错误消息: $msg")
        }
        if (response.optString("projects") == "[]") {
            return jacksonObjectMapper().readValue("{\"ret\":0,\"projects\":{}}")
        }
        return jacksonObjectMapper().readValue(response.toString())
    }
}

// fun main(args: Array<String>) {
//    val secretId = "tBQNvtJiwxglMLI3"
//    val secretKey = "TH5YkuT9rJoyyA31"
//
// //    val (secretId, secretKey) = CommonUtils.getCredential("johuang")
//
//    val client = WeTestClient(secretId, secretKey)
//    val params = HashMap<String, Any>()
//
// //    client.getCloudDevices("0", "1", "0")
// //    println(client.getMyCloud())
// //    println(client.getCloudDevices("7", "1", "0"))
// //    println(client.getTestInfo("1"))
// //    println(CommonUtils.getCredential("johuang"))
// //    println(client.getSession("johuang"))
//
//    println(client.getSession("johuang"))
// }
