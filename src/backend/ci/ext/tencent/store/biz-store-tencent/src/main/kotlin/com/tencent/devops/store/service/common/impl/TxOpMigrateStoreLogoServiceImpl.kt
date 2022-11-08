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

package com.tencent.devops.store.service.common.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TLogo
import com.tencent.devops.model.store.tables.TStoreMediaInfo
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.store.dao.TxMigrateStoreLogoDao
import com.tencent.devops.store.service.common.TxOpMigrateStoreLogoService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.util.concurrent.Executors

@Service
class TxOpMigrateStoreLogoServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val txMigrateStoreLogoDao: TxMigrateStoreLogoDao,
    private val client: Client
) : TxOpMigrateStoreLogoService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpMigrateStoreLogoServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    override fun migrateStoreLogo(): Boolean {
        // 迁移插件logo
        migrateAtomLogo()
        // 迁移模板logo
        migrateTemplateLogo()
        // 迁移IDE插件logo
        migrateIdeAtomLogo()
        // 迁移镜像logo
        migrateImageLogo()
        // 迁移微扩展logo
        migrateExtServiceLogo()
        // 迁移范畴logo
        migrateCategoryLogo()
        // 迁移公共logo
        migrateLogo()
        // 迁移媒体logo
        migrateMediaLogo()
        return true
    }

    private fun migrateAtomLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateAtomLogo!!")
            var offset = 0
            do {
                // 查询插件logo信息记录
                val atomLogoRecords = txMigrateStoreLogoDao.getAtomLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TAtom.T_ATOM
                atomLogoRecords?.forEach { atomLogoRecord ->
                    val logoUrl = atomLogoRecord[tAtom.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = atomLogoRecord[tAtom.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新插件的logo
                    val id = atomLogoRecord[tAtom.ID]
                    txMigrateStoreLogoDao.updateAtomLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateAtomLogo!!")
        }
    }

    private fun checkLogoUrlCondition(logoUrl: String?) =
        logoUrl.isNullOrBlank() || logoUrl.contains("staticfile") || !logoUrl.startsWith("http")

    private fun migrateTemplateLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateTemplateLogo!!")
            var offset = 0
            do {
                // 查询模板logo信息记录
                val templateLogoRecords = txMigrateStoreLogoDao.getTemplateLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tTemplate = TTemplate.T_TEMPLATE
                templateLogoRecords?.forEach { templateLogoRecord ->
                    val logoUrl = templateLogoRecord[tTemplate.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = templateLogoRecord[tTemplate.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新模板的logo
                    val id = templateLogoRecord[tTemplate.ID]
                    txMigrateStoreLogoDao.updateTemplateLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (templateLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateTemplateLogo!!")
        }
    }

    private fun migrateIdeAtomLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateIdeAtomLogo!!")
            var offset = 0
            do {
                // 查询IDE插件logo信息记录
                val atomLogoRecords = txMigrateStoreLogoDao.getIdeAtomLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TIdeAtom.T_IDE_ATOM
                atomLogoRecords?.forEach { atomLogoRecord ->
                    val logoUrl = atomLogoRecord[tAtom.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = atomLogoRecord[tAtom.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新IDE插件的logo
                    val id = atomLogoRecord[tAtom.ID]
                    txMigrateStoreLogoDao.updateIdeAtomLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateIdeAtomLogo!!")
        }
    }

    private fun migrateImageLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateImageLogo!!")
            var offset = 0
            do {
                // 查询镜像logo信息记录
                val imageLogoRecords = txMigrateStoreLogoDao.getImageLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tImage = TImage.T_IMAGE
                imageLogoRecords?.forEach { imageLogoRecord ->
                    val logoUrl = imageLogoRecord[tImage.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = imageLogoRecord[tImage.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新镜像的logo
                    val id = imageLogoRecord[tImage.ID]
                    txMigrateStoreLogoDao.updateImageLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (imageLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateImageLogo!!")
        }
    }

    private fun migrateExtServiceLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateExtServiceLogo!!")
            var offset = 0
            do {
                // 查询微扩展logo信息记录
                val extServiceLogoRecords = txMigrateStoreLogoDao.getExtServiceLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
                extServiceLogoRecords?.forEach { extServiceLogoRecord ->
                    val logoUrl = extServiceLogoRecord[tExtensionService.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = extServiceLogoRecord[tExtensionService.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新微扩展的logo
                    val id = extServiceLogoRecord[tExtensionService.ID]
                    txMigrateStoreLogoDao.updateExtServiceLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (extServiceLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateExtServiceLogo!!")
        }
    }

    private fun migrateCategoryLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateCategoryLogo!!")
            var offset = 0
            do {
                // 查询范畴logo信息记录
                val categoryLogoRecords = txMigrateStoreLogoDao.getCategoryLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tCategory = TCategory.T_CATEGORY
                categoryLogoRecords?.forEach { categoryLogoRecord ->
                    val logoUrl = categoryLogoRecord[tCategory.ICON_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = categoryLogoRecord[tCategory.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新范畴的logo
                    val id = categoryLogoRecord[tCategory.ID]
                    txMigrateStoreLogoDao.updateCategoryLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (categoryLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateCategoryLogo!!")
        }
    }

    private fun migrateLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateLogo!!")
            var offset = 0
            do {
                // 查询商店logo信息记录
                val storeLogoRecords = txMigrateStoreLogoDao.getStoreLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tLogo = TLogo.T_LOGO
                storeLogoRecords?.forEach { storeLogoRecord ->
                    val logoUrl = storeLogoRecord[tLogo.LOGO_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = storeLogoRecord[tLogo.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新商店的logo
                    val id = storeLogoRecord[tLogo.ID]
                    txMigrateStoreLogoDao.updateStoreLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (storeLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateLogo!!")
        }
    }

    private fun migrateMediaLogo() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateMediaLogo!!")
            var offset = 0
            do {
                // 查询媒体信息logo信息记录
                val mediaLogoRecords = txMigrateStoreLogoDao.getMediaLogos(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tStoreMediaInfo = TStoreMediaInfo.T_STORE_MEDIA_INFO
                mediaLogoRecords?.forEach { mediaLogoRecord ->
                    val logoUrl = mediaLogoRecord[tStoreMediaInfo.MEDIA_URL]
                    if (checkLogoUrlCondition(logoUrl)) {
                        return@forEach
                    }
                    val userId = mediaLogoRecord[tStoreMediaInfo.CREATOR]
                    val bkRepoLogoUrl = getBkRepoLogoUrl(logoUrl, userId)
                    if (bkRepoLogoUrl.isNullOrBlank()) {
                        return@forEach
                    }
                    // 更新媒体信息的logo
                    val id = mediaLogoRecord[tStoreMediaInfo.ID]
                    txMigrateStoreLogoDao.updateMediaLogo(dslContext, id, bkRepoLogoUrl)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (mediaLogoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateMediaLogo!!")
        }
    }

    private fun getBkRepoLogoUrl(logoUrl: String, userId: String): String? {
        val fileType = getFileType(logoUrl)
        val tmpFile = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        val fileName = tmpFile.name
        try {
            // 从s3下载logo
            OkhttpUtils.downloadFile(logoUrl, tmpFile)
            // 把logo上传至bkrepo
            val serviceUrlPrefix = client.getServiceUrl(ServiceBkRepoResource::class)
            val destPath = "file/$fileType/$fileName"
            val serviceUrl =
                "$serviceUrlPrefix/service/bkrepo/statics/file/upload?userId=$userId&destPath=$destPath"
            OkhttpUtils.uploadFile(serviceUrl, tmpFile).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.warn("$userId upload file:$fileName fail,responseContent:$responseContent")
                }
                val result = JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
                logger.info("requestUrl:$serviceUrl,result:$result")
                return result.data
            }
        } catch (ignore: Throwable) {
            logger.warn("$userId upload file:$fileName fail, error is:", ignore)
        } finally {
            // 删除临时文件
            tmpFile.delete()
        }
        return null
    }

    private fun getFileType(logoUrl: String): String {
        val paramIndex = logoUrl.lastIndexOf("?")
        val url = if (paramIndex > 0) logoUrl.substring(0, paramIndex) else logoUrl
        val index = url.lastIndexOf(".")
        return url.substring(index + 1).toLowerCase()
    }
}
