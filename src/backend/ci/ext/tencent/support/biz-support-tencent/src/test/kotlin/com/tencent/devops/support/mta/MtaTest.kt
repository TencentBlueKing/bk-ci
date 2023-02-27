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

package com.tencent.devops.support.mta

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.Disabled
import java.util.HashMap
import java.util.concurrent.TimeUnit
import java.security.NoSuchAlgorithmException
import java.security.MessageDigest

class MtaTest {

    @Disabled
    @Test
    fun test() {
        val parameters = mutableListOf<Pair<String, String>>()
        val secretKey = "691375dca8716e014f5b9b3e58d7bed0"
        parameters.add(Pair("app_id", "500630653"))
        parameters.add(Pair("start_date", "2018-08-07"))
        parameters.add(Pair("end_date", "2018-08-13"))
        parameters.add(Pair("urls", "test.devops.oa.com/console/pipeline/,test.devops.oa.com/console/codelib/"))
        parameters.add(Pair("idx", "pv,uv,vv,iv"))
        var signString = secretKey
        val sortedParameters = parameters.sortedBy { it.first }
        for ((key, value) in sortedParameters) {
            signString += key
            signString += "="
            signString += value
        }
//        signString += getUrlParamsByMap(sortedParameters)
        var sing = DigestUtils.md5Hex(signString)

        var sing32 = encode(signString)
        System.out.println(sing)
        parameters.add(Pair("sign", sing))
        var urlParamterString = this.getUrlParamsByMap(parameters)
//        var url = "http://mta.qq.com/h5/api/ctr_core_data?" + urlParamterString
        var url = "http://mta.qq.com/h5/api/ctr_page?" + urlParamterString

        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        var httpClient = OkHttpClient.Builder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .writeTimeout(60L, TimeUnit.SECONDS)
                .build()

        httpClient.newCall(request).execute().use { response ->
            val data = response.body!!.string()
            println(response.code)
            println("response data: $data")
        }
    }

    @Disabled
    @Test
    fun test2() {
        var parameters = mutableListOf<Pair<String, String>>()
        var secretKey = "691375dca8716e014f5b9b3e58d7bed0"
        parameters.add(Pair("app_id", "500630653"))
        parameters.add(Pair("start_date", "2018-08-07"))
        parameters.add(Pair("end_date", "2018-08-13"))
        parameters.add(Pair("idx", "pv,uv,vv,iv"))
        var signString = secretKey
        var sortedParameters = parameters.sortedBy { it.first }
        for ((key, value) in sortedParameters) {
            signString += key
            signString += "="
            signString += value
        }
//        signString += getUrlParamsByMap(sortedParameters)
        var sing = DigestUtils.md5Hex(signString)

        var sing32 = encode(signString)
        System.out.println(sing)
        parameters.add(Pair("sign", sing))
        var urlParamterString = this.getUrlParamsByMap(parameters)
        var url = "http://mta.qq.com/h5/api/ctr_core_data?" + urlParamterString

        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        var httpClient = OkHttpClient.Builder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .writeTimeout(60L, TimeUnit.SECONDS)
                .build()

        httpClient.newCall(request).execute().use { response ->
            val data = response.body!!.string()
            println(response.code)
            println("response data: $data")
        }
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
    fun getUrlParamsByMap(list: List<Pair<String, String>>): String {
        if (list == null) {
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

    fun encode(password: String): String {
        try {
            val instance: MessageDigest = MessageDigest.getInstance("MD5") // 获取md5加密对象
            val digest: ByteArray = instance.digest(password.toByteArray()) // 对字符串加密，返回字节数组
            var sb: StringBuffer = StringBuffer()
            for (b in digest) {
                var i: Int = b.toInt() and 0xff // 获取低八位有效值
                var hexString = Integer.toHexString(i) // 将整数转化为16进制
                if (hexString.length < 2) {
                    hexString = "0" + hexString // 如果是一位的话，补0
                }
                sb.append(hexString)
            }
            return sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return ""
    }
}
