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

package com.tencent.devops.store.service.atom.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.dao.TxAtomEnvDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.TxOpMigrateAtomService
import okhttp3.Credentials
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.Executors

@Service
class TxOpMigrateAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val txAtomEnvDao: TxAtomEnvDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeMemberDao: StoreMemberDao,
    private val client: Client
) : TxOpMigrateAtomService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxOpMigrateAtomServiceImpl::class.java)
        private const val DEFAULT_PAGE_SIZE = 100
    }

    @Value("\${devopsGateway.idc:}")
    private lateinit var devopsIdcGateway: String

    @Value("\${jfrog.url:}")
    private lateinit var jfrogUrl: String

    @Value("\${jfrog.username:}")
    private lateinit var jfrogUsername: String

    @Value("\${jfrog.password:}")
    private lateinit var jfrogPassword: String

    override fun migrateAtomPkg(endTime: String): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateAtomPkg!!")
            var offset = 0
            do {
                // 查询插件环境信息记录
                val atomEnvRecords = txAtomEnvDao.getAtomEnvsByEndTime(
                    dslContext = dslContext,
                    endTime = DateTimeUtil.stringToLocalDateTime(endTime),
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                atomEnvRecords?.forEach { atomEnvRecord ->
                    val pkgPath = atomEnvRecord.pkgPath
                    if (pkgPath.isNullOrBlank()) {
                        return@forEach
                    }
                    // 1、从jfrog下载插件包
                    val pkgParts = pkgPath.split("/")
                    val atomCode = pkgParts[0]
                    val version = pkgParts[1]
                    val pkgName = pkgParts[2]
                    val file = File("/tmp/bk-file/${UUIDUtil.generate()}/$pkgName")
                    try {
                        // 查找插件的初始化项目
                        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                            dslContext = dslContext,
                            storeCode = atomCode,
                            storeType = StoreTypeEnum.ATOM.type.toByte()
                        )
                        val jfrogFileUrl = "$devopsIdcGateway/jfrog/storage/service/atom/$initProjectCode/$pkgPath"
                        OkhttpUtils.downloadFile(jfrogFileUrl, file)
                        // 2、上传插件包至bkrepo
                        val repoName = BkRepoEnum.PLUGIN.repoName
                        uploadFileToBkrepo(
                            repoName = repoName,
                            initProjectCode = initProjectCode,
                            atomCode = atomCode,
                            version = version,
                            destPath = pkgPath,
                            file = file
                        )
                    } catch (ignored: Throwable) {
                        logger.warn("migrateAtomPkg file:$pkgPath failed", ignored)
                    } finally {
                        // 删除临时文件
                        file.delete()
                    }
                }
                offset += DEFAULT_PAGE_SIZE
            } while (atomEnvRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end migrateAtomPkg!!")
        }
        return true
    }

    private fun uploadFileToBkrepo(
        repoName: String,
        initProjectCode: String?,
        atomCode: String,
        version: String,
        destPath: String,
        file: File
    ) {
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val uploadFileUrl = "$serviceUrlPrefix/service/artifactories/store/file/repos/$repoName" +
            "/projects/$initProjectCode/types/ATOM/codes/$atomCode/versions/$version/" +
            "archive?destPath=$destPath"
        val userId = storeMemberDao.getAdmins(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )[0].username
        OkhttpUtils.uploadFile(uploadFileUrl, file, mapOf(AUTH_HEADER_USER_ID to userId)).use { response ->
            val responseContent = response.body()!!.string()
            logger.error("BKSystemErrorMonitor|uploadFile|$destPath|error=$responseContent")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun migrateAtomStaticFile(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin migrateAtomStaticFile!!")
            // 从jfrog获取插件静态文件信息
            val jfrogFileUrl = "$jfrogUrl/api/storage/generic-local/bk-plugin-fe?list" +
                "&deep=1&depth=5&listFolders=0&mdTimestamps=1&includeRootPath=0"
            val headers = mapOf("Authorization" to Credentials.basic(jfrogUsername, jfrogPassword))
            val resp = OkhttpUtils.doGet(jfrogFileUrl, headers)
            val responseStr = resp.body()!!.string()
            val dataMap: Map<String, Any> = jacksonObjectMapper().readValue(responseStr)
            val files = dataMap["files"] as? List<Map<String, Any>>
            files?.forEach { fileMap ->
                val fileUrl = fileMap["uri"] as String
                val fileUrlParts = fileUrl.removePrefix("/").split("/")
                val atomCode = fileUrlParts[0]
                val version = fileUrlParts[1]
                val fileName = fileUrlParts[2]
                val file = File("/tmp/bk-file/${UUIDUtil.generate()}/$fileName")
                try {
                    OkhttpUtils.downloadFile("$jfrogUrl/generic-local/bk-plugin-fe/$fileUrl", file, headers)
                    // 把静态文件上传至bkrepo
                    val repoName = BkRepoEnum.STATIC.repoName
                    // 查找插件的初始化项目
                    val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte()
                    )
                    val destPath = "bk-store/bk-plugin-fe/$fileUrl"
                    uploadFileToBkrepo(
                        repoName = repoName,
                        initProjectCode = initProjectCode,
                        atomCode = atomCode,
                        version = version,
                        destPath = destPath,
                        file = file
                    )
                } catch (ignored: Throwable) {
                    logger.error("BKSystemErrorMonitor|uploadStaticFile|$atomCode|error=${ignored.message}", ignored)
                } finally {
                    // 删除临时文件
                    file.delete()
                }
            }
            logger.info("end migrateAtomStaticFile!!")
        }
        return true
    }
}
