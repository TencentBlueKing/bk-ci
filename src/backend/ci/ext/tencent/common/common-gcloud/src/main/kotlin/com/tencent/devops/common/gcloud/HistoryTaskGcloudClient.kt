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
import com.tencent.devops.common.api.param.ReqParam
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.gcloud.api.pojo.ActionParam
import com.tencent.devops.common.gcloud.api.pojo.CommonParam
import com.tencent.devops.common.gcloud.api.pojo.GcloudListResult
import com.tencent.devops.common.gcloud.api.pojo.GcloudResult
import com.tencent.devops.common.gcloud.api.pojo.ModuleParam
import com.tencent.devops.common.gcloud.api.pojo.history.GetUploadTaskStatParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewAppParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewResParam
import com.tencent.devops.common.gcloud.api.pojo.history.NewUploadTaskParam
import com.tencent.devops.common.gcloud.api.pojo.history.PrePublishParam
import com.tencent.devops.common.gcloud.api.pojo.history.QueryVersionParam
import com.tencent.devops.common.gcloud.api.pojo.history.UploadUpdateFileParam
import com.tencent.devops.common.gcloud.utils.GcloudUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

class HistoryTaskGcloudClient constructor(
    private val objectMapper: ObjectMapper,
    private val host: String,
    private val fileHost: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(HistoryTaskGcloudClient::class.java)
    }

    fun newUploadTask(newUploadAppParam: NewUploadTaskParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, newUploadAppParam.beanToMap(), commonParam, ModuleParam.CENTER, ActionParam.NewUploadTask)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudRequest(request, newUploadAppParam, "newUploadTask")
    }

    fun uploadUpdateFile(file: File, uploadUpdateFileParam: UploadUpdateFileParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, uploadUpdateFileParam.beanToMap(), commonParam, ModuleParam.FILE, ActionParam.UploadUpdateFile)
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
        return doGcloudRequest(request, uploadUpdateFileParam, "uploadUpdateFile")
    }

    fun getUploadTaskStat(getUploadTaskParam: GetUploadTaskStatParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, getUploadTaskParam.beanToMap(), commonParam, ModuleParam.CENTER, ActionParam.GetUploadTaskStat)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudRequest(request, getUploadTaskParam, "getUploadTaskStat")
    }

    fun newRes(newResParam: NewResParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, newResParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.NewRes)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudRequest(request, newResParam, "newRes")
    }

    fun newApp(newAppParam: NewAppParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, newAppParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.NewApp)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudRequest(request, newAppParam, "newApp")
    }

    fun prePublish(prePublishParam: PrePublishParam, commonParam: CommonParam): GcloudResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, prePublishParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.PrePublish)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudRequest(request, prePublishParam, "prePublish")
    }

    fun queryVersion(queryVersionParam: QueryVersionParam, commonParam: CommonParam): GcloudListResult {
        val uri = GcloudUtil.getRequestUriWithSignature(host, fileHost, queryVersionParam.beanToMap(), commonParam, ModuleParam.UPDATE, ActionParam.QueryVersion)
        val request = Request.Builder()
                .url(uri)
                .get()
                .build()
        return doGcloudListRequest(request, queryVersionParam, "queryVersion")
    }

    private fun doGcloudRequest(request: Request, params: ReqParam, keyWord: String): GcloudResult {
        logger.info("$keyWord url: ${request.url.toUrl()}")
        logger.info("$keyWord param: $params")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body!!.string()
            logger.info("$keyWord response>> $uploadResp")
            if (!res.isSuccessful) throw RuntimeException("$keyWord fail:\n$uploadResp")
            val response = objectMapper.readValue<GcloudResult>(uploadResp)
            if (response.code == 0) {
                return response
            } else {
                throw RuntimeException("$keyWord fail, msg: $response:\n$uploadResp")
            }
        }
    }

    private fun doGcloudListRequest(request: Request, params: ReqParam, keyWord: String): GcloudListResult {
        logger.info("$keyWord url: ${request.url.toUrl()}")
        logger.info("$keyWord param: $params")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body!!.string()
            logger.info("$keyWord response>> $uploadResp")
            if (!res.isSuccessful) throw RuntimeException("$keyWord fail:\n$uploadResp")
            val response = objectMapper.readValue<GcloudListResult>(uploadResp)
            if (response.code == 0) {
                return response
            } else {
                throw RuntimeException("$keyWord fail, msg: $response:\n$uploadResp")
            }
        }
    }
}
