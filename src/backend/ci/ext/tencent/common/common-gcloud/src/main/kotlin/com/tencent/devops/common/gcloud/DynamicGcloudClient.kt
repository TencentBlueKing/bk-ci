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
import com.tencent.devops.common.gcloud.api.pojo.GetUploadTaskParam
import com.tencent.devops.common.gcloud.api.pojo.ModuleParam
import com.tencent.devops.common.gcloud.api.pojo.PrePublishParam
import com.tencent.devops.common.gcloud.api.pojo.UploadResParam
import com.tencent.devops.common.gcloud.api.pojo.dyn.DynDeleteVerParam
import com.tencent.devops.common.gcloud.api.pojo.dyn.DynNewResourceParam
import com.tencent.devops.common.gcloud.api.pojo.dyn.DynUpdateVerParam
import com.tencent.devops.common.gcloud.utils.GcloudUtil.getRequestUriWithSignature
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

class DynamicGcloudClient constructor(
    private val objectMapper: ObjectMapper,
    private val host: String,
    private val fileHost: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DynamicGcloudClient::class.java)
    }

    fun uploadDynamicRes(file: File, uploadResParam: UploadResParam, commonParam: CommonParam): Pair<Int/*task_id*/, String/*filepath*/> {
        // 上传动态资源版本
        val uri = getRequestUriWithSignature(host, fileHost, uploadResParam.beanToMap(), commonParam, ModuleParam.FILE, ActionParam.UploadDynamicRes)
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
        logger.info("dyn upload res url: $uri")
        logger.info("dyn get upload res params: $uploadResParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body!!.string()
            logger.info("dyn uploadRes response>> $uploadResp")
            if (!res.isSuccessful) throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn upload res ${file.path} fail:\n$uploadResp")
            val response: Map<String, Any> = objectMapper.readValue(uploadResp)
            if (response["code"] == 0) {
                val result = response["result"] as Map<*, *>
                return Pair(result["task_id"] as Int, result["filepath"] as String)
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn upload res ${file.path} fail, msg: ${response["message"] as String}:\n$uploadResp")
            }
        }
    }

    fun getUploadTask(taskID: Int, commonParam: CommonParam): Map<String, String> {

        val uri = getRequestUriWithSignature(host, fileHost, GetUploadTaskParam(taskID).beanToMap(), commonParam, ModuleParam.FILE, ActionParam.GetUploadTask)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("dyn get upload task url: $uri")
        logger.info("dyn get upload task params: $commonParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()

            logger.info("dyn getUploadTask response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn getUploadTask for taskId $taskID fail:\n$data")
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
                    errorMsg = "dyn getUploadTask for taskId $taskID fail:\n$data")
            }
        }
    }

    fun newResource(newResParam: DynNewResourceParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, newResParam.beanToMap(), commonParam, ModuleParam.DYNUPDATE, ActionParam.NewRes)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()

        logger.info("dyn newResource app url: $uri")
        logger.info("dyn newResource params: $newResParam")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("dyn newResource response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn newResource fail:\n$data")
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn newResource fail:\n$data")
            }
        }
    }

    fun prePublish(prePublishParam: PrePublishParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, prePublishParam.beanToMap(), commonParam, ModuleParam.DYNUPDATE, ActionParam.PrePublish)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        logger.info("dyn pre publish url: $uri")
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("dyn newApp response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn newApp fail:\n$data")
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "dyn newApp fail:\n$data")
            }
        }
    }

    fun publish(): Pair<Boolean, String>? {

        return null
    }

    fun deleteVersion(delVerParam: DynDeleteVerParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, delVerParam.beanToMap(), commonParam, ModuleParam.DYNUPDATE, ActionParam.DeleteRes)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        OkhttpUtils.doLongHttp(request).use { res ->
            val data = res.body!!.string()
            logger.info("delete version response>> $data")
            if (!res.isSuccessful) throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "delete version fail:\n$data")
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "delete version fail:\n$data")
            }
        }
    }

    fun updateVersion(updateVerParam: DynUpdateVerParam, commonParam: CommonParam): String {
        val uri = getRequestUriWithSignature(host, fileHost, updateVerParam.beanToMap(), commonParam, ModuleParam.DYNUPDATE, ActionParam.UpdateRes)
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
                    errorMsg = "update version fail:\n$data")
            val response: Map<String, Any> = objectMapper.readValue(data)
            if (response["code"] == 0) {
                return response["message"] as String
            } else {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "update version fail:\n$data")
            }
        }
    }
}
