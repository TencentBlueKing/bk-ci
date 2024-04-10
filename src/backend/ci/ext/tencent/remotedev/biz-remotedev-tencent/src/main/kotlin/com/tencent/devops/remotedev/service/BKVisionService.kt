package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionDatasetQueryBody
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionResp
import com.tencent.devops.remotedev.pojo.bkvision.QueryFieldDataBody
import com.tencent.devops.remotedev.pojo.bkvision.QueryVariableDataBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BKVisionService @Autowired constructor(
    private val bkConfig: BkConfig,
    private val objectMapper: ObjectMapper
) {
    fun metaQuery(
        shareUid: String,
        type: String
    ): BkVisionResp {
        val url = "${bkConfig.bkvisionUrl}/v1/meta/query?share_uid=${bkConfig.bkvisionShareId}&type=$type"
        val req = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
            .get()
            .build()
        return try {
            OkhttpUtils.doHttp(req).use { response ->
                val data = response.body!!.string()
                logger.debug("metaQuery｜$url|${headerStr()}|${response.code}|$data")
                if (!response.isSuccessful) {
                    logger.error("metaQuery｜$url|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                        params = arrayOf("/meta/query", response.code.toString())
                    )
                }

                val resp = objectMapper.readValue<BkVisionResp>(data)
                resp
            }
        } catch (e: Exception) {
            logger.error("metaQuery request error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                params = arrayOf("/meta/query", e.localizedMessage)
            )
        }
    }

    fun queryDataset(
        body: BkVisionDatasetQueryBody
    ): BkVisionResp {
        val url = "${bkConfig.bkvisionUrl}/v1/dataset/query"
        val req = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return try {
            OkhttpUtils.doHttp(req).use { response ->
                val data = response.body!!.string()
                logger.debug("queryDataset｜$url|${headerStr()}|${response.code}|$data")
                if (!response.isSuccessful) {
                    logger.error("queryDataset｜$url|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                        params = arrayOf("/dataset/query", response.code.toString())
                    )
                }

                val resp = objectMapper.readValue<BkVisionResp>(data)
                resp
            }
        } catch (e: Exception) {
            logger.error("queryDataset request error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                params = arrayOf("/dataset/query", e.localizedMessage)
            )
        }
    }

    fun queryFieldData(
        uid: String,
        body: QueryFieldDataBody
    ): BkVisionResp {
        val url = "${bkConfig.bkvisionUrl}/v1/field/$uid/preview_data/"
        val req = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return try {
            OkhttpUtils.doHttp(req).use { response ->
                val data = response.body!!.string()
                logger.debug("queryFieldData｜$url|${headerStr()}|${response.code}|$data")
                if (!response.isSuccessful) {
                    logger.error("queryFieldData｜$url|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                        params = arrayOf("/field/$uid/preview_data", response.code.toString())
                    )
                }

                val resp = objectMapper.readValue<BkVisionResp>(data)
                resp
            }
        } catch (e: Exception) {
            logger.error("queryFieldData request error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                params = arrayOf("/field/$uid/preview_data", e.localizedMessage)
            )
        }
    }

    fun queryVariableData(
        body: QueryVariableDataBody
    ): BkVisionResp {
        val url = "${bkConfig.bkvisionUrl}/v1/variable/query"
        val req = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr())
            .post(JsonUtil.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        return try {
            OkhttpUtils.doHttp(req).use { response ->
                val data = response.body!!.string()
                logger.debug("queryVariableData｜$url|${headerStr()}|${response.code}|$data")
                if (!response.isSuccessful) {
                    logger.error("queryVariableData｜$url|${response.code}|$data")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                        params = arrayOf("/variable/query", response.code.toString())
                    )
                }

                val resp = objectMapper.readValue<BkVisionResp>(data)
                resp
            }
        } catch (e: Exception) {
            logger.error("queryVariableData request error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_BKVISION_ERROR.errorCode,
                params = arrayOf("/variable/query", e.localizedMessage)
            )
        }
    }

    private fun headerStr(): String {
        return objectMapper.writeValueAsString(
            mapOf("bk_app_code" to bkConfig.appCode, "bk_app_secret" to bkConfig.appSecret)
        ).replace("\\s".toRegex(), "")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BKVisionService::class.java)
    }
}
