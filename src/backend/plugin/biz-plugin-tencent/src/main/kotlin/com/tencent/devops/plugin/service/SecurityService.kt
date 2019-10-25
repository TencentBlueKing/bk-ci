package com.tencent.devops.plugin.service

import com.google.gson.JsonParser
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UnicodeUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.pojo.security.UploadParams
import okhttp3.MultipartBody
import okhttp3.Request
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDateTime

@Service
class SecurityService @Autowired constructor(private val rabbitTemplate: RabbitTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityService::class.java)
        private val jsonPraser = JsonParser()
    }

    @Value("\${mtp.url}")
    private val mtpUrl = ""

    @Value("\${gateway.url}")
    private lateinit var gatewayUrl: String

    @Deprecated("not use", ReplaceWith("noEnvUpload"))
    fun upload(fileStream: InputStream, envId: String, fileName: String, projectId: String, buildId: String, elementId: String): String {
        return ""
    }

    fun noEnvUpload(uploadParams: UploadParams): String {
        logger.info("resource security upload params for build (${uploadParams.buildId}): $uploadParams")

        // 调上传接口
        val taskId = uploadFile(uploadParams)

        // 调启动任务接口
        startTask(taskId, uploadParams)

        return taskId
    }

    private fun uploadFile(uploadParams: UploadParams): String {
        val nonce = RandomStringUtils.randomAlphabetic(16)
        val timestamp = LocalDateTime.now().timestamp()

        val content = "f10e8a9d890de4bd|$timestamp|$nonce"
        val token = FileUtil.getMD5(content)

        with(uploadParams) {
            val metaData = "projectId=$projectId;pipelineId=$pipelineId;buildId=$buildId;userId=$userId;buildNo=$buildNo;" +
                    "source=pipeline;appVersion=$appVersion;appTitle=$appTitle;bundleIdentifier=$packageName;apk.shell.status=true"

            val params = mapOf("page_type" to "add_from_artifactory",
                    "project_code" to projectId,
                    "shell_sln_id" to envId,
                    "apk_md5" to fileMd5,
                    "apk_size" to fileSize,
                    "apk_path" to filePath,
                    "artifactory_type" to if (custom) "custom" else "pipeline",
                    "source" to "2",
                    "nonce" to nonce,
                    "token" to token,
                    "timestamp" to timestamp.toString(),
                    "executor" to userId,
                    "meta_data" to metaData)

            logger.info("upload params for build($buildId): $params")

            val bodyBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
            params.forEach { key, value ->
                bodyBuilder.addFormDataPart(key, value)
            }
            val body = bodyBuilder.build()
            val request = Request.Builder()
                    .url("$mtpUrl/cgi/bk/add_shell")
                    .post(body)
                    .build()

            LogUtils.addLine(rabbitTemplate, buildId, "start to upload security file.", elementId, containerId, executeCount)
            OkhttpUtils.doHttp(request).use { response ->
                val data = UnicodeUtil.unicodeToString(response.body()!!.string())
                logger.info("Get the apk response with message ${response.message()} and code ${response.code()}")
                if (!response.isSuccessful || jsonPraser.parse(data).asJsonObject["ret"].asString != "0") {
                    throw RuntimeException("upload file $filePath to $mtpUrl fail:\n$data")
                }
                LogUtils.addLine(rabbitTemplate, buildId, "upload file $filePath to $mtpUrl success:\n$data",
                    elementId, containerId, executeCount)
                return jsonPraser.parse(data).asJsonObject["id"].asString
            }
        }
    }

    private fun startTask(taskId: String, uploadParams: UploadParams): String {
        with(uploadParams) {
            val timestamp = LocalDateTime.now().timestamp()
            val nonce = RandomStringUtils.randomAlphabetic(16)

            val content = "f10e8a9d890de4bd|$timestamp|$nonce"
            val token = FileUtil.getMD5(content)

            val params = mapOf("page_type" to "do_add_shell",
                "project_code" to projectId,
                "shell_sln_id" to envId,
                "apk_id" to taskId,
                "source" to "2",
                "nonce" to nonce,
                "token" to token,
                "timestamp" to timestamp.toString())

            logger.info("startTask params for build($buildId): $params")

            val taskBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("page_type", params["page_type"]!!)
                .addFormDataPart("project_code", params["project_code"]!!)
                .addFormDataPart("shell_sln_id", params["shell_sln_id"]!!)
                .addFormDataPart("apk_id", params["apk_id"]!!)
                .addFormDataPart("source", params["source"]!!)
                .addFormDataPart("nonce", params["nonce"]!!)
                .addFormDataPart("token", params["token"]!!)
                .addFormDataPart("timestamp", params["timestamp"]!!)
                .build()
            val taskRequest = Request.Builder()
                .url("$mtpUrl/cgi/bk/add_shell")
                .post(taskBody)
                .build()

            LogUtils.addLine(rabbitTemplate, buildId, "start security task in env($envId) for task($taskId).",
                elementId, containerId, executeCount)
            OkhttpUtils.doHttp(taskRequest).use { response ->
                val data = UnicodeUtil.unicodeToString(response.body()!!.string())
                if (!response.isSuccessful || jsonPraser.parse(data).asJsonObject["ret"].asString != "0") {
                    throw RuntimeException("fail to start the task[$taskId]:\n$data")
                }
                LogUtils.addLine(rabbitTemplate, buildId, "success to start the task[$taskId]: $$data",
                    elementId, containerId, executeCount)
                return data
            }
        }
    }

    fun getFinalResult(projectId: String, envId: String, buildId: String, elementId: String, taskId: String): String {
        val nonce = RandomStringUtils.randomAlphabetic(16)
        val timestamp = LocalDateTime.now().timestamp()

        val content = "f10e8a9d890de4bd|$timestamp|$nonce"
        val token = FileUtil.getMD5(content)

        val params = mapOf("page_type" to "get_result_url",
                "project_code" to projectId,
                "shell_sln_id" to envId,
                "apk_id" to taskId,
                "source" to "2",
                "nonce" to nonce,
                "token" to token,
                "timestamp" to timestamp.toString())

        logger.info("getFinalResult params for build($buildId): $params")

        val taskBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("page_type", params["page_type"]!!)
                .addFormDataPart("project_code", params["project_code"]!!)
                .addFormDataPart("shell_sln_id", params["shell_sln_id"]!!)
                .addFormDataPart("apk_id", params["apk_id"]!!)
                .addFormDataPart("source", params["source"]!!)
                .addFormDataPart("nonce", params["nonce"]!!)
                .addFormDataPart("token", params["token"]!!)
                .addFormDataPart("timestamp", params["timestamp"]!!)
                .build()
        val taskRequest = Request.Builder()
                .url("$mtpUrl/cgi/bk/add_shell")
                .post(taskBody)
                .build()

        OkhttpUtils.doHttp(taskRequest).use { response ->
            val data = UnicodeUtil.unicodeToString(response.body()!!.string())
            if (!response.isSuccessful) {
                throw RuntimeException("fail to get task[$taskId] result:\n$data")
            }
            return data
        }
    }

    // 获取jfrog传回的url
    private fun getUrl(projectId: String, realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "http://$gatewayUrl/jfrog/storage/service/custom/$projectId$realPath"
        } else {
            "http://$gatewayUrl/jfrog/storage/service/archive/$projectId$realPath"
        }
    }
}

// fun main(args: Array<String>) {
//    var nonce = RandomStringUtils.randomAlphabetic(16)
//    var timestamp = LocalDateTime.now().timestamp()
//
//    var content = "f10e8a9d890de4bd|$timestamp|$nonce"
//    var token = DigestUtils.md5Hex(content)
//
//    val file = File("d:/temp/s-123.apk")
//    val jsonPraser = JsonParser()
//    //调上传接口
//    val body = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("page_type","add_apk_info")
//            .addFormDataPart("project_code","a90")
//            .addFormDataPart("shell_sln_id","7")
//            .addFormDataPart("apk_md5", DigestUtils.md5Hex(file.inputStream()))
//            .addFormDataPart("apk_size",file.length().toString())
//            .addFormDataPart("apk_file",file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file))
//            .addFormDataPart("source","2")
//            .addFormDataPart("no_cert","1")
//            .addFormDataPart("nonce", nonce)
//            .addFormDataPart("token",token)
//            .addFormDataPart("timestamp",timestamp.toString())
//            .build()
//    val request = Request.Builder()
//            .url("http://mtpt.oa.com/cgi/upload/add_shell_bk")
//            .post(body)
//            .build()
//    var taskId = ""
//
//    //{"info": "ok", "apk_md5": "7c9a168a0c39c1f0b4639f9c747b3b49", "app_name": "SODA", "tpshell_cfg": {"so_list": ["libunity.so", "libmain.so", "libmono.so"], "dll_list": ["System.dll", "mscorlib.dll", "Mono.Security.dll", "Assembly-CSharp.dll", "UnityEngine.Networking.dll", "UnityEngine.dll", "UnityEngine.UI.dll", "System.Core.dll", "Assembly-CSharp-firstpass.dll"], "enable_partial_record": 0, "auto_add_mtp": 1, "disable_shell": 0}, "must_select": {"so_list": ["libmono.so"], "dll_list": ["Assembly-CSharp.dll", "Assembly-CSharp-firstpass.dll"]}, "ret": 0, "app_ver": "3.2.6", "app_file_info": {"so": ["libBugly.so", "libitlogin.so", "libtpnsSecurity.so", "libtpnsWatchdog.so"], "dll": []}, "id": 5064, "can_custom_shell": 0}
//    OkhttpUtils.doHttp(request).use { response ->
//        file.delete()
//        val data = response.body()!!.string()
//        println(data)
//        if (!response.isSuccessful || jsonPraser.parse(data).asJsonObject["ret"].asString != "0") {
//            throw RuntimeException("fail")
//        }
//        taskId = jsonPraser.parse(data).asJsonObject["id"].asString
//    }
//
//
//    println()
//    println()
//    println()
//
//    //调启动任务接口
//    nonce = RandomStringUtils.randomAlphabetic(16)
//    timestamp = LocalDateTime.now().timestamp()
//
//    content = "f10e8a9d890de4bd|$timestamp|$nonce"
//    token = DigestUtils.md5Hex(content)
//    val map = mapOf("page_type" to "do_add_shell",
//            "project_code" to "a90",
//            "shell_sln_id" to "7",
//            "apk_id" to taskId)
//    val json = ObjectMapper().writeValueAsString(map)
// //    val taskBody = RequestBody.create(MediaType.parse("application/json"), json)
//    val taskBody = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("page_type","do_add_shell")
//            .addFormDataPart("project_code","a90")
//            .addFormDataPart("shell_sln_id","7")
//            .addFormDataPart("apk_id",taskId)
//            .addFormDataPart("source","2")
//            .addFormDataPart("nonce", nonce)
//            .addFormDataPart("token",token)
//            .addFormDataPart("timestamp",timestamp.toString())
//            .build()
//    val taskRequest = Request.Builder()
//            .url("http://mtpt.oa.com/cgi/bk/add_shell")
//            .post(taskBody)
//            .build()
//
//    //{"info": "\u672a\u627e\u5230\u53c2\u6570:page_type", "ret": -1}
//    //{"info": "\u63d0\u4ea4\u52a0\u58f3\u6210\u529f", "ret": 0}
//    OkhttpUtils.doHttp(taskRequest).use { response ->
//        val data = response.body()!!.string()
//        println(data)
//    }
// }