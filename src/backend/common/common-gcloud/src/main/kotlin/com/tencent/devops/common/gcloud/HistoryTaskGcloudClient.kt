package com.tencent.devops.common.gcloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.param.ReqParam
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
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
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
                .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file))
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
        logger.info("$keyWord url: ${request.url().url()}")
        logger.info("$keyWord param: $params")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body()!!.string()
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
        logger.info("$keyWord url: ${request.url().url()}")
        logger.info("$keyWord param: $params")
        OkhttpUtils.doLongHttp(request).use { res ->
            val uploadResp = res.body()!!.string()
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
