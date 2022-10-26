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
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.store.dao.TxOpMigrateStoreDescriptionDao
import com.tencent.devops.store.service.common.TxOpMigrateStoreDescriptionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern

@Service
class TxOpMigrateStoreDescriptionServiceImpl : TxOpMigrateStoreDescriptionService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var txOpMigrateStoreDescriptionDao: TxOpMigrateStoreDescriptionDao

    @Autowired
    lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpMigrateStoreDescriptionServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
        private const val BK_CI_PATH_REGEX = "(!\\[(.*?)]\\()(http[s]?://radosgw.open.oa.com(.*?))(\\))"
    }

    override fun migrateStoreDescription(): Boolean {
        // 迁移插件描述信息引用资源
        migrateAtomDescription()
        // 迁移模板描述信息引用资源
        migrateTemplateDescription()
        // 迁移IDE插件描述信息引用资源
        migrateIdeAtomDescription()
        // 迁移镜像描述信息引用资源
        migrateImageDescription()
        // 迁移微扩展描述信息引用资源
        migrateExtServiceDescription()
        return true
    }

    private fun migrateAtomDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateAtomDescription!!")
            var offset = 0
            do {
                // 查询插件描述信息记录
                val atomDescriptionRecords = txOpMigrateStoreDescriptionDao.getAtomDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TAtom.T_ATOM
                atomDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tAtom.DESCRIPTION]
                    val userId = atomDescriptionRecord[tAtom.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it.replace("?", "\\?")] = bkRepoFileUrl
                    }
                    if (pathMap.isEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新插件的描述
                    val id = atomDescriptionRecord[tAtom.ID]
                    txOpMigrateStoreDescriptionDao.updateAtomDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateAtomDescription!!")
        }
    }

    private fun migrateTemplateDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateTemplateDescription!!")
            var offset = 0
            do {
                // 查询模板描述信息记录
                val templateDescriptionRecords = txOpMigrateStoreDescriptionDao.getTemplateDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tTemplate = TTemplate.T_TEMPLATE
                templateDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tTemplate.DESCRIPTION]
                    val userId = atomDescriptionRecord[tTemplate.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it.replace("?", "\\?")] = bkRepoFileUrl
                    }
                    if (pathMap.isEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新模板的描述
                    val id = atomDescriptionRecord[tTemplate.ID]
                    txOpMigrateStoreDescriptionDao.updateTemplateDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (templateDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateTemplateDescription!!")
        }
    }

    private fun migrateIdeAtomDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateIdeAtomDescription!!")
            var offset = 0
            do {
                // 查询IDE插件描述信息记录
                val atomDescriptionRecords = txOpMigrateStoreDescriptionDao.getIdeAtomDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tAtom = TIdeAtom.T_IDE_ATOM
                atomDescriptionRecords?.forEach { atomDescriptionRecord ->
                    val description = atomDescriptionRecord[tAtom.DESCRIPTION]
                    val userId = atomDescriptionRecord[tAtom.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it.replace("?", "\\?")] = bkRepoFileUrl
                    }
                    if (pathMap.isEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新插件的描述
                    val id = atomDescriptionRecord[tAtom.ID]
                    txOpMigrateStoreDescriptionDao.updateIdeAtomDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateIdeAtomDescription!!")
        }
    }

    private fun migrateImageDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateImageDescription!!")
            var offset = 0
            do {
                // 查询镜像描述信息记录
                val imageDescriptionRecords = txOpMigrateStoreDescriptionDao.getImageDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tImage = TImage.T_IMAGE
                imageDescriptionRecords?.forEach { imageDescriptionRecord ->
                    val description = imageDescriptionRecord[tImage.DESCRIPTION]
                    val userId = imageDescriptionRecord[tImage.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it.replace("?", "\\?")] = bkRepoFileUrl
                    }
                    if (pathMap.isEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新镜像的描述
                    val id = imageDescriptionRecord[tImage.ID]
                    txOpMigrateStoreDescriptionDao.updateImageDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (imageDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateImageDescription!!")
        }
    }

    private fun migrateExtServiceDescription() {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateExtServiceDescription!!")
            var offset = 0
            do {
                // 查询微扩展描述信息记录
                val imageDescriptionRecords = txOpMigrateStoreDescriptionDao.getExtServiceDescription(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                val tExtensionService = TExtensionService.T_EXTENSION_SERVICE
                imageDescriptionRecords?.forEach { imageDescriptionRecord ->
                    val description = imageDescriptionRecord[tExtensionService.DESCRIPTION]
                    val userId = imageDescriptionRecord[tExtensionService.CREATOR]
                    val pathList = checkLogoUrlCondition(description)
                    val pathMap = mutableMapOf<String, String>()
                    if (pathList.isNullOrEmpty()) {
                        return@forEach
                    }
                    pathList.forEach path@{
                        val bkRepoFileUrl = getBkRepoFileUrl(it, userId)
                        if (bkRepoFileUrl.isNullOrBlank()) {
                            return@path
                        }
                        pathMap[it.replace("?", "\\?")] = bkRepoFileUrl
                    }
                    if (pathMap.isEmpty()) {
                        return@forEach
                    }
                    val newDescription = replaceDescription(description, pathMap)
                    // 更新微扩展的描述
                    val id = imageDescriptionRecord[tExtensionService.ID]
                    txOpMigrateStoreDescriptionDao.updateExtServiceDescription(dslContext, id, newDescription)
                }
                offset += DEFAULT_PAGE_SIZE
            } while (imageDescriptionRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateExtServDescription!!")
        }
    }

    fun checkLogoUrlCondition(description: String?): List<String>? {
        if (description.isNullOrBlank()) {
            return null
        }
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(description)
        val pathList = mutableListOf<String>()
        while (matcher.find()) {
            pathList.add(matcher.group(3))
        }
        return pathList
    }

    private fun getFileType(fileUrl: String): String {
        val paramIndex = fileUrl.lastIndexOf("?")
        val url = if (paramIndex > 0) fileUrl.substring(0, paramIndex) else fileUrl
        val index = url.lastIndexOf(".")
        return url.substring(index + 1).toLowerCase()
    }

    fun replaceDescription(description: String, pathMap: Map<String, String>): String {
        var newDescription = description
        pathMap.forEach {
            val pattern: Pattern = Pattern.compile("(!\\[(.*)]\\()(${it.key})(\\))")
            val matcher: Matcher = pattern.matcher(newDescription)
            newDescription = matcher.replaceAll("$1${it.value}$4")
        }
        return newDescription
    }

    private fun getBkRepoFileUrl(fileUrl: String, userId: String): String? {
        val fileType = getFileType(fileUrl)
        val tmpFile = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        val fileName = tmpFile.name
        try {
            OkhttpUtils.downloadFile(fileUrl, tmpFile)
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
}
