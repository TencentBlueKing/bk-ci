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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.service

import com.google.common.io.Files
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.plugin.client.WeTestClient
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import net.dongliu.apk.parser.ApkFile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class WetestService @Autowired constructor(
    private val jfrogService: JfrogService,
    private val wetestTaskInstResultService: WetestTaskInstResultService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestService::class.java)
    }

    fun uploadRes(accessId: String, accessToken: String, type: String, fileParams: ArtifactorySearchParam): Map<String, Any> {
        val wetestClient = WeTestClient(accessId, accessToken)
        // 下载文件
        val tmpFolder = Files.createTempDir()
        try {
            val file = jfrogService.downloadFile(fileParams, tmpFolder.canonicalPath).firstOrNull()
                    ?: throw OperationException("0 file found in file path: ${fileParams.regexPath}(${fileParams.custom})")
            // 保存任务
            val map = wetestClient.uploadRes(type, file).toMap().toMutableMap()

            // 解析返回apkVersion
            if (file.canonicalPath.endsWith(".apk")) {
                map["meta.versionName"] = getApkVersion(file)
            }
            return map
        } finally {
            tmpFolder.deleteRecursively()
        }
    }

    private fun getApkVersion(file: File): String {
        try {
            val apkFile = ApkFile(file)
            val meta = apkFile.apkMeta
            return meta.versionName
        } catch (e: Exception) {
            logger.info("解析apk包( ${file.canonicalPath} )失败: ${e.message}｝")
        }
        return ""
    }

    fun autoTest(accessId: String, accessToken: String, request: WetestAutoTestRequest): Map<String, Any> {
        val wetestClient = WeTestClient(accessId, accessToken)
        return wetestClient.autoTest(request).toMap()
    }

    fun queryTestStatus(accessId: String, accessToken: String, testId: String): Map<String, Any> {
        val wetestClient = WeTestClient(accessId, accessToken)
        return wetestClient.testStatus(testId).toMap()
    }

    fun taskCallback(testId: String, callback: Map<String, Any>): String {
        return wetestTaskInstResultService.saveResult(testId, callback)
    }
}

// fun main(args: Array<String>) {
//    val secretId = "tBQNvtJiwxglMLI3"
//    val secretKey = "TH5YkuT9rJoyyA31"
//
//    val client = WeTestClient(secretId, secretKey)
//    {msg=, ret=0, apkid=837812}
//    {msg=, ret=0, ipaid=2424}
//    val result = client.uploadRes("apk", File("d:/temp/SODA.ipa")).toMap()
//    println(result)

//    {"msg":"","ret":0,"scriptid":467010}
//    val result =client.uploadRes("script", File("d:/temp/wetest/hello.zip"))
//    println(result)

//    apk: {ret=0, msg=, testid=d37e02f94459b29c52a80b71acc9131c, reporturl=http://wetest.qq.com/cloud/report/result?testid=d37e02f94459b29c52a80b71acc9131c}
//    {ret=0, msg=, testid=4210d2b35dd8894426f2527cc3b75bd9, reporturl=http://wetest.qq.com/cloud/report/result?testid=4210d2b35dd8894426f2527cc3b75bd9}
//    ipa: {ret=0, msg=, testid=8bb6c1bfaee4c703b1e3e65167264826, reporturl=http://wetest.qq.com/cloud/report/result?testid=8bb6c1bfaee4c703b1e3e65167264826}
//        val request = WetestAutoTestRequest(
//            837812,
//            null ,
//            467010,
//            2,
//            5,
//            "install"
//    )
//    println(JSONObject(request))
//    val result = client.autoTest(request).toMap()
//    println(result)

//    {"ret":0,"msg":"","teststatus":{"enddate":"进行中","endtime":0,"testid":"d37e02f94459b29c52a80b71acc9131c","starttime":1544260602,"startdate":"2018-12-08  17:16:42","isdone":false,"type":0,"typedesc":"标准兼容测试"}}
//    {"ret":0,"msg":"","teststatus":{"enddate":"2018-12-08  17:26:35","endtime":1544261195,"testid":"d37e02f94459b29c52a80b71acc9131c","starttime":1544260602,"startdate":"2018-12-08  17:16:42","isdone":true,"type":0,"typedesc":"标准兼容测试"}}
//    val result = client.testStatus("d37e02f94459b29c52a80b71acc9131c")
//    println(result)
// }