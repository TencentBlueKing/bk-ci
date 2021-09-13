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

package com.tencent.devops.support.util.mta.h5

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.support.model.mta.h5.enums.IdxType
import com.tencent.devops.support.model.mta.h5.message.CoreDataMessage
import com.tencent.devops.support.model.mta.h5.result.CoreDataResult
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import java.text.SimpleDateFormat
import java.util.Date

object MTAH5Util {

    private val appID = "500630653"
    private val secretKey = "691375dca8716e014f5b9b3e58d7bed0"
    private val apiURL = "http://mta.qq.com/h5/api"
    private val idxDefault = "pv,uv,vv,iv"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val requestCoreDataApiURL = "$apiURL/ctr_core_data" // 应用历史趋势

    private var mapper = jacksonObjectMapper()
//    private var httpClient = OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()

    fun getCoreData(coreDataMessage: CoreDataMessage): CoreDataResult? {
        var coreDataReponse: CoreDataResult? = null
        val parameters = mutableListOf<Pair<String, String>>()
        parameters.add(Pair("app_id", "500630653"))
        val startDate = dateFormat.format(Date(coreDataMessage.startDate))
        val endDate = dateFormat.format(Date(coreDataMessage.endDate))
        parameters.add(Pair("start_date", startDate))
        parameters.add(Pair("end_date", endDate))
        parameters.add(Pair("idx", idxDefault))
        var signString = secretKey
        val sortedParameters = parameters.sortedBy { it.first }
        for ((key, value) in sortedParameters) {
            signString += key
            signString += "="
            signString += value
        }
        val sign = DigestUtils.md5Hex(signString)
        parameters.add(Pair("sign", sign))
        val urlParameterString = this.getUrlParamsByList(parameters)
        val url = "$requestCoreDataApiURL?$urlParameterString"
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
//        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            try {

                coreDataReponse = mapper.readValue(responseContent)
            } catch (e: Exception) {
                return CoreDataResult(0, "success", null)
            }
        }

        return coreDataReponse
    }

    /**
     * 将url参数转换成map
     * @param param aa=11&bb=22&cc=33
     * @return
     */
    fun getUrlParams(param: String): Map<String, Any> {
        val map = HashMap<String, Any>(0)
        if (param.isEmpty()) {
            return map
        }
        val params = param.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in params.indices) {
            val p = params[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (p.size == 2) {
                map[p[0]] = p[1]
            }
        }
        return map
    }

    /**
     * 将map转换成url
     * @param map
     * @return
     */
    fun getUrlParamsByList(list: List<Pair<String, String>>): String {
        if (list.isEmpty()) {
            return ""
        }
        val sb = StringBuffer()
        for ((key, value) in list) {
            sb.append("$key=$value")
            sb.append("&")
        }
        var s = sb.toString()
        if (s.endsWith("&")) {
            s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, "&")
        }
        return s
    }

    /**
     * 将map转换成url
     * @param map
     * @return
     */
    fun getIdxByList(list: List<IdxType>): String {
        if (list.isEmpty()) {
            return ""
        }
        val sb = StringBuffer()
        for (value in list) {
            sb.append(value.toString())
            sb.append(",")
        }
        var s = sb.toString()
        if (s.endsWith(",")) {
            s = org.apache.commons.lang3.StringUtils.substringBeforeLast(s, ",")
        }
        return s
    }
}
