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

package com.tencent.devops.common.gcloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.gcloud.api.pojo.ActionParam
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.DeleteVerParam
import com.tencent.devops.common.gcloud.api.pojo.GetUploadTaskParam
import com.tencent.devops.common.gcloud.api.pojo.ModuleParam
import com.tencent.devops.common.gcloud.api.pojo.NewAppParam
import com.tencent.devops.common.gcloud.api.pojo.NewResourceParam
import com.tencent.devops.common.gcloud.api.pojo.PrePublishParam
import com.tencent.devops.common.gcloud.api.pojo.UpdateVerParam
import com.tencent.devops.common.gcloud.api.pojo.UploadAppParam
import com.tencent.devops.common.gcloud.api.pojo.UploadResParam
import com.tencent.devops.common.gcloud.utils.GcloudUtil.getRequestUriWithSignature
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

class GcloudClient constructor(
    private val objectMapper: ObjectMapper,
    private val host: String,
    private val fileHost: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GcloudClient::class.java)
    }

    fun uploadApp(file: File, uploadAppParam: UploadAppParam, commonParam: CommonParam): Pair<Int/*task_id*/, String/*filepath*/> {
        // 上传文件
        val uri = getRequestUriWithSignature(host, fileHost, uploadAppParam.beanToMap(), commonParam, ModuleParam.FILE, ActionParam.UploadApp)
        val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, RequestBody.create(
                    "application/octet-stream".toMediaTypeOrNull(),
                    file
                ))
                .build()
        val request = Request.Builder()
                .url(uri)
                .post(body)
                .build()
        logger.info("upload app url: $uri")
        logger.info("upload app param: $uploadAppParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body!!.string()
            logger.info("uploadApp response>> $uploadResp")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "upload file ${file.path} fail:\n$uploadResp"
            )
            val response: Map<String, Any> = objectMapper.readValue(uploadResp)
            if (response["code"] == 0) {
                val result = response["result"] as Map<*, *>
                return Pair(result["task_id"] as Int, result["filepath"] as String)
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "upload file ${file.path} fail, msg: ${response["message"] as String}:\n$uploadResp"
                )
            }
        }
    }

    fun uploadRes(file: File, uploadResParam: UploadResParam, commonParam: CommonParam): Pair<Int/*task_id*/, String/*filepath*/> {
        // 上传文件
        val uri = getRequestUriWithSignature(host, fileHost, uploadResParam.beanToMap(), commonParam, ModuleParam.FILE, ActionParam.UploadRes)
        val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, RequestBody.create(
                    "application/octet-stream".toMediaTypeOrNull(),
                    file
                ))
                .build()
        val request = Request.Builder()
                .url(uri)
                .post(body)
                .build()
        logger.info("upload res url: $uri")
        logger.info("get upload res params: $uploadResParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body!!.string()
            logger.info("uploadRes response>> $uploadResp")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "upload res ${file.path} fail:\n$uploadResp"
            )
            val response: Map<String, Any> = objectMapper.readValue(uploadResp)
            if (response["code"] == 0) {
                val result = response["result"] as Map<*, *>
                return Pair(result["task_id"] as Int, result["filepath"] as String)
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "upload res ${file.path} fail, msg: ${response["message"] as String}:\n$uploadResp"
                )
            }
        }
    }

    fun getUploadTask(taskID: Int, commonParam: CommonParam): Map<String, String> {

        val uri = getRequestUriWithSignature(host, fileHost, GetUploadTaskParam(taskID).beanToMap(), commonParam, ModuleParam.FILE, ActionParam.GetUploadTask)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("get upload task url: $uri")
        logger.info("get upload task params: $commonParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()

            logger.info("getUploadTask response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "getUploadTask for taskId $taskID fail:\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                val result = response["result"] as Map<*, *>
                val taskId = result["TaskID"] as Int
                val state = result["State"] as String
                val versionInfo = if (result["VersionInfo"] == null) "" else result["VersionInfo"] as String
                val versionStr = if (result["VersionString"] == null) "" else result["VersionString"] as String
                val message = response["message"] as String
                return mapOf("taskId" to taskId.toString(),
                        "state" to state,
                        "versionInfo" to versionInfo,
                        "versionStr" to versionStr,
                        "message" to message)
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "getUploadTask for taskId $taskID fail:\n$data"
                )
            }
        }
    }

    fun newApp(newAppParam: NewAppParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, newAppParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.NewApp)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()

        logger.info("new app url: $uri")
        logger.info("newApp params: $newAppParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("newApp response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "newApp fail\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "newResource fail:\n$data"
                )
            }
        }
    }

    fun newResource(newResParam: NewResourceParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, newResParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.NewRes)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()

        logger.info("newResource app url: $uri")
        logger.info("newResource params: $newResParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("newResource response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "newResource fail:\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "newResource fail:\n$data"
                )
            }
        }
    }

    fun prePublish(prePublishParam: PrePublishParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, prePublishParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.PrePublish)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("pre publish url: $uri")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("newApp response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "newApp fail:\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "newApp fail:\n$data"
                )
            }
        }
    }

    fun publish(): Pair<Boolean, String>? {
        return null
    }

    fun updateVersion(updateVerParam: UpdateVerParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, updateVerParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.UpdateVersion)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("update version url: $uri")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("update version response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "update version fail:\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "update version fail:\n$data"
                )
            }
        }
    }

    fun deleteVersion(delVerParam: DeleteVerParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, delVerParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.DeleteVersion)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("update version url: $uri")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("update version response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "update version fail:\n$data"
            )
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "update version fail:\n$data"
                )
            }
        }
    }
}

// fun main(argv: Array<String>) {
//    var varMap = TreeMap<String, String>()
//    varMap["gameid"] = "1592421477"
//    varMap["ts"] = System.currentTimeMillis().toString()
//    varMap["nonce"] = Random().nextInt(Int.MAX_VALUE).toString()
//    varMap["accessid"] = "uRmOoeH2aPdCSTxxvzgyXY3gVHYE"
//    varMap["productid"] = "1201"
//    varMap["versionstr"] = "1.0.0.1"
//    varMap["md5"] = "7c9a168a0c39c1f0b4639f9c747b3b49"
//
//    val accessKey = "6hgLdCAaqeSxaLlGs2MKYixrGwYE"
//    val host = "10.247.139.161:8081"
//    val urlPrefix = "/v1/openapi"
//    val module = "file"
//    val action = "UploadApp"
//
//    val uri = "$urlPrefix/$module/$action"
//    var reqUri = "http://$host$uri?"
//    var sig = "$uri?"
//
//    var isFirst = true
//    varMap.forEach {
//        if (isFirst) {
//            isFirst = false
//        } else {
//            sig += "&"
//            reqUri += "&"
//        }
//        sig += "${it.key}=${it.value}"
//        reqUri += "${it.key}=${it.value}"
//    }
//    logger.info(sig)
//    sig = hmacMD5Encode(accessKey, sig)
//    sig = sig.replace("+", "-")
//    sig = sig.replace("/", "_")
//
//    reqUri += "&signature=$sig"
//    logger.info(reqUri)
//
// //    {
// //        "code": 0,
// //        "result": {
// //        "filepath": "1592421477/5_1592421477_1201_1.0.0.1_1532433899.apk",
// //        "md5": "7c9a168a0c39c1f0b4639f9c747b3b49",
// //        "task_id": 19232
// //    },
// //        "nonce": "1099238625",
// //        "message": "success",
// //        "ts": "1532433851333"
// //    }
//
// }
