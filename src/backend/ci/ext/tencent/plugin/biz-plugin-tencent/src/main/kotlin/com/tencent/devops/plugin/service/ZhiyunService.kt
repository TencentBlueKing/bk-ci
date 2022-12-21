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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.zhiyun.ZhiyunConfig
import com.tencent.devops.model.plugin.tables.TPluginZhiyunProduct
import com.tencent.devops.plugin.dao.ZhiyunProductDao
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ZhiyunService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val zhiyunProductDao: ZhiyunProductDao,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter,
    private val zhiyunConfig: ZhiyunConfig,
    private val bkRepoClient: BkRepoClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ZhiyunService::class.java)
    }

    fun pushFile(zhiyunUploadParam: ZhiyunUploadParam): List<String> {
        val fileParams = zhiyunUploadParam.fileParams
        logger.info("zhi yun upload param for build(${fileParams.buildId}): $zhiyunUploadParam")

        val tmpFolder = Files.createTempDir()
        try {
            val matchFiles = bkRepoClient.downloadFileByPattern(
                userId = zhiyunUploadParam.operator,
                projectId = fileParams.projectId,
                pipelineId = fileParams.pipelineId,
                buildId = fileParams.buildId,
                repoName = if (fileParams.custom) "custom" else "pipeline",
                pathPattern = fileParams.regexPath,
                destPath = tmpFolder.canonicalPath
            )
            if (matchFiles.isEmpty()) throw OperationException("0 file find in ${fileParams.regexPath}" +
                "(custom: ${fileParams.custom})")
            val resultList = mutableListOf<String>()
            matchFiles.forEach { file ->
                try {
                    buildLogPrinter.addLine(fileParams.buildId, "start upload file to zhiyun: ${file.canonicalPath}",
                        fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                    val request = with(zhiyunUploadParam) {
                        buildLogPrinter.addLine(fileParams.buildId, "zhiyun upload file params: $para",
                            fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                        val body = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("tarball", file.name,
                                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file))
                            .addFormDataPart("caller", zhiyunConfig.caller)
                            .addFormDataPart("password", zhiyunConfig.password)
                            .addFormDataPart("operator", operator)
                            .addFormDataPart("para[product]", para.product)
                            .addFormDataPart("para[name]", para.name)
                            .addFormDataPart("para[author]", para.author)
                            .addFormDataPart("para[description]", para.description)
                            .addFormDataPart("para[clean]", para.clean)
                            .addFormDataPart("para[ciInstId]", para.buildId)
                            .addFormDataPart("para[codeUrl]", para.codeUrl!!)
                            .build()
                        Request.Builder()
                            .header("apikey", zhiyunConfig.apiKey)
                            .url("${zhiyunConfig.url}/simpleCreateVersion")
                            .post(body)
                            .build()
                    }
                    OkhttpUtils.doHttp(request).use { res ->
                        val response = res.body!!.string()
                        logger.info("zhi yun upload response for build(${fileParams.buildId}): $response")
                        val jsonMap = objectMapper.readValue<Map<String, Any>>(response)
                        val code = jsonMap["code"]
                        val msg = jsonMap["msg"] as String
                        if (code != "0") {
                            throw OperationException("fail to upload \" ${file.canonicalPath} \":\n$msg")
                        }
                        buildLogPrinter.addLine(fileParams.buildId, "successfully upload: ${file.name}:\n$msg",
                            fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                        resultList.add(msg.trim().removeSuffix(":succ"))
                    }
                } finally {
                    file.delete()
                }
            }
            return resultList
        } finally {
            tmpFolder.deleteRecursively()
        }
    }

    fun getList(): List<ZhiyunProduct> {
        val recordList = zhiyunProductDao.getList(dslContext)
        val result = mutableListOf<ZhiyunProduct>()
        if (recordList != null) {
            with(TPluginZhiyunProduct.T_PLUGIN_ZHIYUN_PRODUCT) {
                for (item in recordList) {
                    result.add(
                        ZhiyunProduct(
                            productId = item.get(PRODUCT_ID),
                            productName = item.get(PRODUCT_NAME)
                        )
                    )
                }
            }
        }
        return result
    }

    fun createProduct(zhiyunProduct: ZhiyunProduct) {
        zhiyunProductDao.save(dslContext, zhiyunProduct.productId, zhiyunProduct.productName)
    }

    fun deleteProduct(productId: String) {
        zhiyunProductDao.delete(dslContext, productId)
    }
}
