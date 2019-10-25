package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.model.plugin.tables.TPluginZhiyunProduct
import com.tencent.devops.plugin.config.ZhiyunConfig
import com.tencent.devops.plugin.dao.ZhiyunProductDao
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunProduct
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ZhiyunService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val jfrogService: JfrogService,
    private val zhiyunProductDao: ZhiyunProductDao,
    private val dslContext: DSLContext,
    private val rabbitTemplate: RabbitTemplate
) {

    @Value("\${zhiyun.url}")
    val url: String = ""

    @Value("\${zhiyun.caller}")
    val caller: String = ""

    @Value("\${zhiyun.password}")
    val password: String = ""

    @Value("\${zhiyun.apiKey}")
    val apiKey: String = ""

    @Value("\${zhiyun.esbUrl}")
    val esbUrl: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(ZhiyunService::class.java)
    }

    fun pushFile(zhiyunUploadParam: ZhiyunUploadParam): List<String> {
        val fileParams = zhiyunUploadParam.fileParams
        logger.info("zhi yun upload param for build(${fileParams.buildId}): $zhiyunUploadParam")

        val tmpFolder = Files.createTempDir()
        try {
            val matchFiles = jfrogService.downloadFile(fileParams, tmpFolder.canonicalPath)
            if (matchFiles.isEmpty()) throw OperationException("There is 0 file find in ${fileParams.regexPath}(custom: ${fileParams.custom})")
            val resultList = mutableListOf<String>()
            matchFiles.forEach { file ->
                        try {
                            LogUtils.addLine(rabbitTemplate, fileParams.buildId, "start to upload file to zhi yun: ${file.canonicalPath}",
                                fileParams.elementId, fileParams.executeCount)
                            val request = with(zhiyunUploadParam) {
                                LogUtils.addLine(rabbitTemplate, fileParams.buildId, "zhi yun upload file params: $para",
                                    fileParams.elementId, fileParams.executeCount)
                                val body = MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("tarball", file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file))
                                        .addFormDataPart("caller", caller)
                                        .addFormDataPart("password", password)
                                        .addFormDataPart("operator", operator)
                                        .addFormDataPart("para[product]", para.product)
                                        .addFormDataPart("para[name]", para.name)
                                        .addFormDataPart("para[author]", para.author)
                                        .addFormDataPart("para[description]", para.description)
                                        .addFormDataPart("para[clean]", para.clean)
                                        .addFormDataPart("para[ciInstId]", para.buildId)
                                        .addFormDataPart("para[codeUrl]", para.codeUrl)
                                        .build()
                                Request.Builder()
                                        .header("apikey", apiKey)
                                        .url("${url}/simpleCreateVersion")
                                        .post(body)
                                        .build()
                            }
                            OkhttpUtils.doHttp(request).use { res ->
                                val response = res.body()!!.string()
                                logger.info("zhi yun upload response for build(${fileParams.buildId}): $response")
                                val jsonMap = objectMapper.readValue<Map<String, Any>>(response)
                                val code = jsonMap["code"]
                                val msg = jsonMap["msg"] as String
                                if (code != "0") {
                                    throw OperationException("fail to upload \" ${file.canonicalPath} \":\n$msg")
                                }
                                LogUtils.addLine(rabbitTemplate, fileParams.buildId, "successfully upload: ${file.name}:\n$msg",
                                    fileParams.elementId, fileParams.executeCount)
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
