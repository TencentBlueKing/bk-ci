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