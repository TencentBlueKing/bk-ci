/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.service

import com.tencent.devops.artifactory.pojo.ArchiveStorePkgRequest
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.KEY_OS_NAME
import com.tencent.devops.common.api.constant.KEY_SUMMARY
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.common.dao.CategoryDao
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.LabelDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreFileService.Companion.fileSeparator
import com.tencent.devops.store.common.utils.StoreFileAnalysisUtil
import com.tencent.devops.store.constant.StoreMessageCode.STORE_COMPONENT_CONFIG_YML_FORMAT_ERROR
import com.tencent.devops.store.constant.StoreMessageCode.STORE_PACKAGE_FILE_NOT_FOUND
import com.tencent.devops.store.constant.StoreMessageCode.STORE_VERSION_IS_NOT_FINISH
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_FILE_PATH_ERROR
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
import com.tencent.devops.store.pojo.common.BK_STORE_FIRST_PUBLISHER_FLAG
import com.tencent.devops.store.pojo.common.CONFIG_YML_NAME
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_DEFAULT_FLAG
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.KEY_STORE_PACKAGE_FILE
import com.tencent.devops.store.pojo.common.StoreReleaseBaseInfo
import com.tencent.devops.store.pojo.common.StoreReleaseInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreBaseUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.common.version.VersionModel
import java.io.File
import java.io.InputStream
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.FileSystemUtils

abstract class StorePackageDeployService {

    companion object {
        private val logger = LoggerFactory.getLogger(StorePackageDeployService::class.java)
    }
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeReleaseService: StoreReleaseService
    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao
    @Autowired
    lateinit var storeLogoService: StoreLogoService
    @Autowired
    lateinit var storeFileService: StoreFileService
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var labelDao: LabelDao
    @Autowired
    lateinit var categoryDao: CategoryDao
    @Autowired
    lateinit var classifyDao: ClassifyDao

    /**
     * 一键发布组件
     * @return 发布组件返回报文
     */
    fun oneClickDeployComponent(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): String? {
        val (storeDirPath, storePackageFile) = StoreFileAnalysisUtil.extractStorePackage(
            storeCode = storeCode,
            storeType = storeType,
            inputStream = inputStream,
            disposition = disposition
        )
        try {
            val bkConfigMap = StoreFileAnalysisUtil.getBkConfigMap(storeDirPath)
            if (bkConfigMap.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = STORE_PACKAGE_FILE_NOT_FOUND,
                    params = arrayOf(CONFIG_YML_NAME)
                )
            }
            bkConfigMap[KEY_STORE_PACKAGE_FILE] = storePackageFile
            val codeCount = storeBaseQueryDao.countByCondition(
                dslContext = dslContext,
                storeType = storeType,
                storeCode = storeCode
            )
            val firstPublisherFlag = codeCount == 0
            if (firstPublisherFlag) {
                storeReleaseService.createComponent(
                    userId = userId,
                    storeCreateRequest = getStoreCreateRequest(storeCode, storeType, bkConfigMap)
                )?.let {
                    bkConfigMap[KEY_STORE_ID] = it.storeId
                }
            } else {
                val record = storeBaseQueryDao.getLatestComponentByCode(dslContext, storeCode, storeType)
                if ((record != null) && record.status !in StoreStatusEnum.getStoreFinalStatusList()) {
                    throw ErrorCodeException(
                        errorCode = STORE_VERSION_IS_NOT_FINISH,
                        params = arrayOf(record.storeCode, record.version)
                    )
                }
            }
            bkConfigMap[BK_STORE_FIRST_PUBLISHER_FLAG] = firstPublisherFlag
            // 检查bk-config.yml配置
            val checkBkConfigResult = checkBkConfig(bkConfigMap)
            if (checkBkConfigResult.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = STORE_COMPONENT_CONFIG_YML_FORMAT_ERROR,
                    params = checkBkConfigResult.toTypedArray()
                )
            }
            if (bkConfigMap[KEY_RELEASE_INFO] == null) {
                return null
            }
            // 组件发布前准备
            storeUpdatePreBus(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                storeDirPath = storeDirPath,
                bkConfigMap = bkConfigMap
            )
            storeReleaseService.updateComponent(userId, getStoreUpdateRequest(storeCode, storeType, bkConfigMap))?.let {
                bkConfigMap[KEY_STORE_ID] = it.storeId
            }
            return bkConfigMap[KEY_STORE_ID]?.toString()
        } finally {
            storePackageFile.delete()
            FileSystemUtils.deleteRecursively(File(storeDirPath).parentFile)
        }
    }

    private fun checkBkConfig(bkConfigMap: MutableMap<String, Any>): List<String> {
        val osInfoList = bkConfigMap[KEY_OS] as? List<Map<String, Any>>
        val voidFields = mutableListOf<String>()
        if (osInfoList.isNullOrEmpty()) {
            voidFields.add(KEY_OS)
        }
        // 校验os必填字段
        osInfoList?.forEachIndexed { index, osInfo ->
            if (osInfo[KEY_OS_NAME] == null) {
                voidFields.add("$KEY_OS.$index.$KEY_OS_NAME")
            }
            if (osInfo[KEY_PACKAGE_PATH] == null) {
                voidFields.add("$KEY_OS.$index.$KEY_PACKAGE_PATH")
            }
            if (osInfo[KEY_DEFAULT_FLAG] == null) {
                voidFields.add("$KEY_OS.$index.$KEY_DEFAULT_FLAG")
            }
        }
        try {
            val firstPublisherFlag = bkConfigMap[BK_STORE_FIRST_PUBLISHER_FLAG] as Boolean
            val storeReleaseInfo = bkConfigMap[KEY_RELEASE_INFO] as? StoreReleaseInfo
            if (firstPublisherFlag && storeReleaseInfo?.baseInfo != null) {
                voidFields.addAll(validateBaseInfo(storeReleaseInfo.baseInfo))
            }
            storeReleaseInfo?.baseInfo?.let {
                voidFields.addAll(checkStoreReleaseExtInfo(storeReleaseInfo))
            }
        } catch (ignored: Throwable) {
            logger.warn("checkBkConfig $KEY_RELEASE_INFO parse failed", ignored)
            voidFields.add(KEY_RELEASE_INFO)
        }
        return voidFields
    }

    abstract fun checkStoreReleaseExtInfo(storeReleaseInfo: StoreReleaseInfo): List<String>

    private fun validateBaseInfo(baseInfo: StoreReleaseBaseInfo): List<String> {
        return listOfNotNull(
            baseInfo.name.takeIf { it.isNullOrBlank() }?.let { NAME },
            baseInfo.classifyCode.takeIf { it.isNullOrBlank() }?.let { KEY_CLASSIFY_CODE },
            baseInfo.summary.takeIf { it.isNullOrBlank() }?.let { KEY_SUMMARY }
        ).map { "$KEY_RELEASE_INFO.baseInfo.$it" }
    }

    private fun getStoreCreateRequest(
        storeCode: String,
        storeType: StoreTypeEnum,
        bkConfigMap: Map<String, Any>
    ): StoreCreateRequest {
        val storeReleaseInfo = bkConfigMap[KEY_RELEASE_INFO] as StoreReleaseInfo
        return StoreCreateRequest(
            projectCode = storeReleaseInfo.projectId,
            baseInfo = StoreBaseCreateRequest(
                storeCode = storeCode,
                storeType = storeType,
                name = storeReleaseInfo.baseInfo.name!!,
                extBaseInfo = storeReleaseInfo.baseInfo.extBaseInfo,
                baseFeatureInfo = storeReleaseInfo.baseInfo.baseFeatureInfo,
                baseEnvInfos = storeReleaseInfo.baseInfo.baseEnvInfos
            )
        )
    }

    private fun storeUpdatePreBus(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        storeDirPath: String,
        bkConfigMap: MutableMap<String, Any>
    ) {
        val storeReleaseInfo = bkConfigMap[KEY_RELEASE_INFO] as StoreReleaseInfo
        val logoUrl = storeReleaseInfo.baseInfo.logoUrl
        logoUrl?.let {
            storeReleaseInfo.baseInfo.logoUrl = logoUrlAnalysis(userId, storeDirPath, logoUrl)
        }
        storeReleaseInfo.baseInfo.description?.let {
            storeReleaseInfo.baseInfo.description = storeFileService.textReferenceFileAnalysis(
                content = it,
                fileDirPath = "$storeDirPath${fileSeparator}file",
                userId = userId
            )
        }
        bkConfigMap[KEY_STORE_PACKAGE_FILE]?.let {
            val archiveAtomResult = StoreFileAnalysisUtil.serviceArchiveStoreFile(
                userId = userId,
                client = client,
                file = it as File,
                archiveStorePkgRequest = ArchiveStorePkgRequest(
                    storeCode = storeCode,
                    storeType = storeType,
                    version = storeReleaseInfo.baseInfo.versionInfo.version,
                    releaseType = storeReleaseInfo.baseInfo.versionInfo.releaseType
                )
            )
            if (archiveAtomResult.isNotOk()) {
                throw ErrorCodeException(errorCode = USER_UPLOAD_PACKAGE_INVALID)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun logoUrlAnalysis(userId: String, storeDirPath: String, logoUrl: String): String {
        var url = logoUrl
        // 远程logo资源不做处理
        if (!url.startsWith("http")) {
            // 解析logoUrl
            val logoUrlAnalysisResult = StoreFileAnalysisUtil.logoUrlAnalysis(url)
            if (logoUrlAnalysisResult.isNotOk()) {
                return url
            }
            val relativePath = logoUrlAnalysisResult.data
            val logoFile = File(
                "$storeDirPath${File.separator}file" +
                        "${File.separator}${relativePath?.removePrefix(File.separator)}"
            )
            if (logoFile.exists()) {
                val result = storeLogoService.uploadStoreLogo(
                    userId = userId,
                    contentLength = logoFile.length(),
                    inputStream = logoFile.inputStream(),
                    disposition = FormDataContentDisposition(
                        "form-data; name=\"logo\"; filename=\"${logoFile.name}\""
                    )
                )
                if (result.isOk()) {
                    result.data?.logoUrl?.let { url = it }
                }
            } else {
                throw ErrorCodeException(
                    errorCode = USER_UPLOAD_FILE_PATH_ERROR,
                    params = arrayOf(relativePath ?: "")
                )
            }
        }
        return url
    }

    private fun getStoreUpdateRequest(
        storeCode: String,
        storeType: StoreTypeEnum,
        bkConfigMap: Map<String, Any>
    ): StoreUpdateRequest {
        val storeReleaseInfo = bkConfigMap[KEY_RELEASE_INFO] as StoreReleaseInfo
        val firstPublisherFlag = bkConfigMap[BK_STORE_FIRST_PUBLISHER_FLAG] as Boolean
        val baseInfo = storeReleaseInfo.baseInfo
        var name = baseInfo.name
        var summary = baseInfo.summary
        var classifyCode = baseInfo.classifyCode
        // 非首次发布并且必填参数为空时沿用上次发布填写数据
        var releaseType = baseInfo.versionInfo.releaseType
        if (!firstPublisherFlag) {
            val newestComponentInfo = storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeType)!!
            releaseType = handedReleaseType(storeCode, storeType, baseInfo)
            name = name ?: newestComponentInfo.name
            summary = summary ?: newestComponentInfo.summary
            classifyCode =
                classifyCode ?: classifyDao.getClassify(dslContext, newestComponentInfo.classifyId)!!.classifyCode
        }
        val labelIdList = baseInfo.labelCodeList?.let {
            labelDao.getIdsByCodes(dslContext, it, storeType.type.toByte())
        }
        val categoryIdList = baseInfo.categoryCodeList?.let {
            categoryDao.getIdsByCodes(dslContext, it, storeType.type.toByte())
        }
        return StoreUpdateRequest(
            projectCode = storeReleaseInfo.projectId,
            baseInfo = StoreBaseUpdateRequest(
                storeCode = storeCode,
                storeType = storeType,
                name = name!!,
                logoUrl = baseInfo.logoUrl,
                classifyCode = classifyCode!!,
                summary = summary!!,
                description = baseInfo.description,
                versionInfo = VersionModel(
                    publisher = baseInfo.versionInfo.publisher,
                    releaseType = releaseType,
                    version = baseInfo.versionInfo.version,
                    versionContent = baseInfo.versionInfo.versionContent
                ),
                labelIdList = labelIdList?.let { ArrayList(it) },
                categoryIdList = categoryIdList?.let { ArrayList(it) },
                extBaseInfo = baseInfo.extBaseInfo,
                baseFeatureInfo = baseInfo.baseFeatureInfo,
                baseEnvInfos = baseInfo.baseEnvInfos
            )
        )
    }

    /**
     * 处理发布类型
     */
    private fun handedReleaseType(
        storeCode: String,
        storeType: StoreTypeEnum,
        baseInfo: StoreReleaseBaseInfo
    ): ReleaseTypeEnum {
        val version = baseInfo.versionInfo.version
        val status = storeBaseQueryDao.getComponent(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            version = version
        )?.status
        return if (status == StoreStatusEnum.GROUNDING_SUSPENSION.name) {
            ReleaseTypeEnum.CANCEL_RE_RELEASE
        } else {
            baseInfo.versionInfo.releaseType
        }
    }
}
