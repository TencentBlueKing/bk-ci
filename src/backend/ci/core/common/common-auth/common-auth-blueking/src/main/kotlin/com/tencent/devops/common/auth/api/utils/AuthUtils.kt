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

package com.tencent.devops.common.auth.api.utils

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BkAuthProperties
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.TreeMap

class AuthUtils constructor(xBkAuthProperties: BkAuthProperties) {
    private val logger = LoggerFactory.getLogger(AuthUtils::class.java)
    private val host = xBkAuthProperties.url!!

    /**
     * 执行get请求
     */
    fun doAuthGetRequest(uri: String, params: Map<String, Any>?, xBkAppCode: String, xBkAppSecret: String): JSONObject {
        val url = this.getAuthRequestUrl(uri, params)
        logger.debug("bkiam get url: {}", url)

        val request = Request.Builder()
            .url(url)
            .addHeader("X-BK-APP-CODE", xBkAppCode)
            .addHeader("X-BK-APP-SECRET", xBkAppSecret)
            .build()
        val result = this.doRequest(request)
        logger.debug("bkiam get request result: {}", result)
        return result
    }

    /**
     * 执行post请求
     */
    fun doAuthPostRequest(uri: String, jsonbody: JSONObject, bkAppCode: String, bkAppSecret: String): JSONObject {
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonbody.toString())
        val url = this.getAuthRequestUrl(uri, null)
        logger.debug("bkiam post url: {}, body: {}", url, jsonbody)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-BK-APP-CODE", bkAppCode)
            .addHeader("X-BK-APP-SECRET", bkAppSecret)
            .build()
        val result = this.doRequest(request)
        logger.debug("bkiam post request result: {}", result)
        return result
    }

    /**
     * 执行put请求
     */
    fun doAuthPutRequest(uri: String, jsonbody: JSONObject, xBkAppCode: String, xBkAppSecret: String): JSONObject {
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonbody.toString())
        val url = this.getAuthRequestUrl(uri, null)
        logger.debug("bkiam put url: {}, body: {}", url, jsonbody)

        val request = Request.Builder()
            .url(url)
            .put(body)
            .addHeader("X-BK-APP-CODE", xBkAppCode)
            .addHeader("X-BK-APP-SECRET", xBkAppSecret)
            .build()
        val result = this.doRequest(request)
        logger.debug("bkiam put request result: {}", result)
        return result
    }

    /**
     * 执行delete请求
     */
    fun doAuthDeleteRequest(uri: String, jsonbody: JSONObject, xBkAppCode: String, xBkAppSecret: String): JSONObject {
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonbody.toString())
        val url = this.getAuthRequestUrl(uri, null)
        logger.debug("bkiam delete url: {}", url)

        val request = Request.Builder()
            .url(url)
            .delete(body)
            .addHeader("X-BK-APP-CODE", xBkAppCode)
            .addHeader("X-BK-APP-SECRET", xBkAppSecret)
            .build()
        val result = this.doRequest(request)
        logger.debug("bkiam delete request result: {}", result)
        return result
    }

    // 处理请求结果
    private fun doRequest(request: Request): JSONObject {
        var jsonObject = JSONObject()
        try {
            val response = OkhttpUtils.doHttp(request)
//            val response = okclient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body!!.string()
                logger.debug("bkiam response: $responseStr")
                jsonObject = JSONObject(responseStr)

                // 由于成功与失败时返回的json结构不同，code不为0，失败情况需直接去除data字段
                if (jsonObject["data"] !is JSONArray) jsonObject.remove("data")
            } else {
                jsonObject.put("msg", "http code:" + response.code)
                throw RemoteServiceException("bkiam request failed, response: ($response)")
            }

            val responseCode = jsonObject.getInt("code")
            if (responseCode == 0) {
                return jsonObject
            }

            val msg = jsonObject.getString("message")
            jsonObject.put("message", msg)
            jsonObject.put("code", responseCode)
            logger.warn("bkiam failed , message: $msg")
        } catch (ioe: IOException) {
            jsonObject.put("msg", "IO exception, network not ok: ${ioe.message}")
        } catch (je: JSONException) {
            jsonObject.put("msg", "json parse error, bkiam response json format failed: ${je.message}")
        }
        return jsonObject
    }

    /**
     * 生成请求url
     */
    fun getAuthRequestUrl(uri: String, queryParams: Map<String, Any>?): String {
        val params = TreeMap<String, Any>(Comparator<String> { o1, o2 -> o1.compareTo(o2) })

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
            sb.append("$key=$value")
        }
        return host + sb.toString()
    }
}
