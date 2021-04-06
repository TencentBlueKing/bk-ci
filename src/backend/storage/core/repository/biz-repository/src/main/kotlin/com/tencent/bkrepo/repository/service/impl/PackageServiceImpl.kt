/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageUpdateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionUpdateRequest
import com.tencent.bkrepo.repository.search.packages.PackageSearchInterpreter
import com.tencent.bkrepo.repository.service.PackageService
import com.tencent.bkrepo.repository.util.MetadataUtils
import com.tencent.bkrepo.repository.util.PackageQueryHelper
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PackageServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao,
    private val packageSearchInterpreter: PackageSearchInterpreter
) : AbstractService(), PackageService {

    override fun findPackageByKey(projectId: String, repoName: String, packageKey: String): PackageSummary? {
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey)
        return convert(tPackage)
    }

    override fun findVersionByName(
        projectId: String,
        repoName: String,
        packageKey: String,
        versionName: String
    ): PackageVersion? {
        val packageId = packageDao.findByKey(projectId, repoName, packageKey)?.id ?: return null
        return convert(packageVersionDao.findByName(packageId, versionName))
    }

    override fun findVersionNameByTag(
        projectId: String,
        repoName: String,
        packageKey: String,
        tag: String
    ): String? {
        val versionTag = packageDao.findByKey(projectId, repoName, packageKey)?.versionTag ?: return null
        return versionTag[tag]
    }

    override fun findLatestBySemVer(
        projectId: String,
        repoName: String,
        packageKey: String
    ): PackageVersion? {
        val packageId = packageDao.findByKey(projectId, repoName, packageKey)?.id ?: return null
        return convert(packageVersionDao.findLatest(packageId))
    }

    override fun listPackagePage(
        projectId: String,
        repoName: String,
        option: PackageListOption
    ): Page<PackageSummary> {
        val pageNumber = option.pageNumber
        val pageSize = option.pageSize
        Preconditions.checkArgument(pageNumber >= 0, "pageNumber")
        Preconditions.checkArgument(pageSize >= 0, "pageSize")
        val query = PackageQueryHelper.packageListQuery(projectId, repoName, option.packageName)
        val totalRecords = packageDao.count(query)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val records = packageDao.find(query.with(pageRequest)).map { convert(it)!! }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun listAllPackageName(projectId: String, repoName: String): List<String> {
        val query = PackageQueryHelper.packageListQuery(projectId, repoName, null)
        query.fields().include(TPackage::key.name)
        return packageDao.find(query, Map::class.java).map {
            it[TPackage::key.name].toString()
        }
    }

    override fun listVersionPage(
        projectId: String,
        repoName: String,
        packageKey: String,
        option: VersionListOption
    ): Page<PackageVersion> {
        val pageNumber = option.pageNumber
        val pageSize = option.pageSize
        Preconditions.checkArgument(pageNumber >= 0, "pageNumber")
        Preconditions.checkArgument(pageSize >= 0, "pageSize")
        val stageTag = option.stageTag?.split(StringPool.COMMA)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey)
        return if (tPackage == null) {
            Pages.ofResponse(pageRequest, 0, emptyList())
        } else {
            val query = PackageQueryHelper.versionListQuery(tPackage.id!!, option.version, stageTag)
            val totalRecords = packageVersionDao.count(query)
            val records = packageVersionDao.find(query.with(pageRequest)).map { convert(it)!! }
            Pages.ofResponse(pageRequest, totalRecords, records)
        }
    }

    override fun listAllVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        option: VersionListOption
    ): List<PackageVersion> {
        val stageTag = option.stageTag?.split(StringPool.COMMA)
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey) ?: return emptyList()
        val query = PackageQueryHelper.versionListQuery(tPackage.id!!, option.version, stageTag)
        return packageVersionDao.find(query).map { convert(it)!! }
    }

    override fun createPackageVersion(request: PackageVersionCreateRequest) {
        with(request) {
            Preconditions.checkNotBlank(packageKey, this::packageKey.name)
            Preconditions.checkNotBlank(packageName, this::packageName.name)
            Preconditions.checkNotBlank(versionName, this::packageName.name)
            // 先查询包是否存在，不存在先创建包
            val tPackage = findOrCreatePackage(request)
            // 检查版本是否存在
            val oldVersion = packageVersionDao.findByName(tPackage.id!!, versionName)
            val newVersion = if (oldVersion != null) {
                if (!overwrite) {
                    throw ErrorCodeException(ArtifactMessageCode.VERSION_EXISTED, packageName, versionName)
                }
                // overwrite
                oldVersion.apply {
                    lastModifiedBy = request.createdBy
                    lastModifiedDate = LocalDateTime.now()
                    size = request.size
                    manifestPath = request.manifestPath
                    artifactPath = request.artifactPath
                    stageTag = request.stageTag.orEmpty()
                    metadata = MetadataUtils.fromMap(request.metadata)
                    tags = request.tags?.filter { it.isNotBlank() }.orEmpty()
                    extension = request.extension.orEmpty()
                }
            } else {
                // create new
                tPackage.versions += 1
                TPackageVersion(
                    createdBy = createdBy,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = createdBy,
                    lastModifiedDate = LocalDateTime.now(),
                    packageId = tPackage.id!!,
                    name = versionName.trim(),
                    size = size,
                    ordinal = calculateOrdinal(versionName),
                    downloads = 0,
                    manifestPath = manifestPath,
                    artifactPath = artifactPath,
                    stageTag = stageTag.orEmpty(),
                    metadata = MetadataUtils.fromMap(metadata),
                    tags = request.tags?.filter { it.isNotBlank() }.orEmpty(),
                    extension = request.extension.orEmpty()
                )
            }
            packageVersionDao.save(newVersion)
            // 更新包
            tPackage.lastModifiedBy = newVersion.lastModifiedBy
            tPackage.lastModifiedDate = newVersion.lastModifiedDate
            tPackage.description = packageDescription?.let { packageDescription }
            tPackage.latest = versionName
            tPackage.extension = extension?.let { extension }
            tPackage.versionTag = mergeVersionTag(tPackage.versionTag, versionTag)
            packageDao.save(tPackage)

            logger.info("Create package version[$newVersion] success")
        }
    }

    override fun deletePackage(projectId: String, repoName: String, packageKey: String) {
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey) ?: return
        packageVersionDao.deleteByPackageId(tPackage.id!!)
        packageDao.deleteByKey(projectId, repoName, packageKey)
        logger.info("Delete package [$projectId/$repoName/$packageKey] success")
    }

    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, versionName: String) {
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey) ?: return
        val tPackageVersion = packageVersionDao.findByName(tPackage.id.orEmpty(), versionName) ?: return
        packageVersionDao.deleteByName(tPackageVersion.packageId, tPackageVersion.name)
        tPackage.versions -= 1
        if (tPackage.versions <= 0L) {
            packageDao.removeById(tPackage.id.orEmpty())
            logger.info("Delete package [$projectId/$repoName/$packageKey-$versionName] because no version exist")
        } else if (tPackage.latest == tPackageVersion.name) {
            val latestVersion = packageVersionDao.findLatest(tPackage.id.orEmpty())
            tPackage.latest = latestVersion?.name.orEmpty()
            packageDao.save(tPackage)
        }
        logger.info("Delete package version[$projectId/$repoName/$packageKey-$versionName] success")
    }

    override fun updatePackage(request: PackageUpdateRequest) {
        val projectId = request.projectId
        val repoName = request.repoName
        val packageKey = request.packageKey
        val tPackage = checkPackage(projectId, repoName, packageKey).apply {
            name = request.name ?: name
            description = request.description ?: description
            versionTag = request.versionTag ?: versionTag
            extension = request.extension ?: extension
            lastModifiedBy = SecurityUtils.getUserId()
            lastModifiedDate = LocalDateTime.now()
        }
        packageDao.save(tPackage)
    }

    override fun updateVersion(request: PackageVersionUpdateRequest) {
        val projectId = request.projectId
        val repoName = request.repoName
        val packageKey = request.packageKey
        val versionName = request.versionName
        val packageId = checkPackage(projectId, repoName, packageKey).id.orEmpty()
        val tPackageVersion = checkPackageVersion(packageId, versionName).apply {
            size = request.size ?: size
            manifestPath = request.manifestPath ?: manifestPath
            artifactPath = request.artifactPath ?: artifactPath
            stageTag = request.stageTag ?: stageTag
            metadata = request.metadata?.let { MetadataUtils.fromMap(it) } ?: metadata
            tags = request.tags ?: tags
            extension = request.extension ?: extension
            lastModifiedBy = SecurityUtils.getUserId()
            lastModifiedDate = LocalDateTime.now()
        }
        packageVersionDao.save(tPackageVersion)
    }

    override fun downloadVersion(projectId: String, repoName: String, packageKey: String, versionName: String) {
        val tPackage = checkPackage(projectId, repoName, packageKey)
        val tPackageVersion = checkPackageVersion(tPackage.id!!, versionName)
        if (tPackageVersion.artifactPath.isNullOrBlank()) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "artifactPath is null")
        }
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, tPackageVersion.artifactPath!!)
        val context = ArtifactDownloadContext(artifact = artifactInfo)
        ArtifactContextHolder.getRepository().download(context)
    }

    override fun addDownloadRecord(projectId: String, repoName: String, packageKey: String, versionName: String) {
        val tPackage = checkPackage(projectId, repoName, packageKey)
        val tPackageVersion = checkPackageVersion(tPackage.id!!, versionName)
        tPackageVersion.downloads += 1
        packageVersionDao.save(tPackageVersion)
        tPackage.downloads += 1
        packageDao.save(tPackage)
    }

    override fun searchPackage(queryModel: QueryModel): Page<MutableMap<*, *>> {
        val context = packageSearchInterpreter.interpret(queryModel)
        val query = context.mongoQuery
        val countQuery = Query.of(query).limit(0).skip(0)
        val totalRecords = packageDao.count(countQuery)
        val packageList = packageDao.find(query, MutableMap::class.java)
        val pageNumber = if (query.limit == 0) 0 else (query.skip / query.limit).toInt()
        return Page(pageNumber + 1, query.limit, totalRecords, packageList)
    }

    override fun populatePackage(request: PackagePopulateRequest) {
        with(request) {
            // 先查询包是否存在，不存在先创建包
            val tPackage = findOrCreatePackage(request)
            val packageId = tPackage.id.orEmpty()
            var latestVersion = packageVersionDao.findLatest(packageId)
            // 检查版本是否存在
            versionList.forEach {
                if (packageVersionDao.findByName(packageId, it.name) != null) {
                    logger.info("Package version[${tPackage.name}-${it.name}] existed, skip populating.")
                    return@forEach
                }
                val newVersion = TPackageVersion(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate,
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate,
                    packageId = packageId,
                    name = it.name.trim(),
                    size = it.size,
                    ordinal = calculateOrdinal(it.name),
                    downloads = it.downloads,
                    manifestPath = it.manifestPath,
                    artifactPath = it.artifactPath,
                    stageTag = it.stageTag.orEmpty(),
                    metadata = MetadataUtils.fromMap(it.metadata),
                    extension = it.extension.orEmpty()
                )
                packageVersionDao.save(newVersion)
                tPackage.versions += 1
                tPackage.downloads += it.downloads
                tPackage.versionTag = mergeVersionTag(tPackage.versionTag, versionTag)
                if (latestVersion == null) {
                    latestVersion = newVersion
                } else if (it.createdDate.isAfter(latestVersion?.createdDate)) {
                    latestVersion = newVersion
                }
                logger.info("Create package version[$newVersion] success")
            }
            // 更新包
            tPackage.latest = latestVersion?.name ?: tPackage.latest
            packageDao.save(tPackage)
            logger.info("Update package version[$tPackage] success")
        }
    }

    override fun getPackageCount(projectId: String, repoName: String): Long {
        val query = PackageQueryHelper.packageListQuery(projectId, repoName, null)
        return packageDao.count(query)
    }

    /**
     * 查找包，不存在则创建
     *
     */
    private fun findOrCreatePackage(request: PackagePopulateRequest): TPackage {
        with(request) {
            return packageDao.findByKey(projectId, repoName, key) ?: run {
                val tPackage = TPackage(
                    createdBy = createdBy,
                    createdDate = createdDate,
                    lastModifiedBy = lastModifiedBy,
                    lastModifiedDate = lastModifiedDate,
                    projectId = projectId,
                    repoName = repoName,
                    name = name.trim(),
                    description = description,
                    key = key.trim(),
                    type = type,
                    downloads = 0,
                    versions = 0,
                    versionTag = versionTag.orEmpty(),
                    extension = extension.orEmpty()
                )
                try {
                    packageDao.save(tPackage)
                    logger.info("Create package[$tPackage] success")
                    return tPackage
                } catch (exception: DuplicateKeyException) {
                    logger.warn("Create package[$tPackage] error: [${exception.message}]")
                    packageDao.findByKey(projectId, repoName, key)!!
                }
            }
        }
    }

    /**
     * 查找包，不存在则创建
     */
    private fun findOrCreatePackage(request: PackageVersionCreateRequest): TPackage {
        with(request) {
            return packageDao.findByKey(projectId, repoName, packageKey) ?: run {
                val tPackage = TPackage(
                    createdBy = createdBy,
                    createdDate = LocalDateTime.now(),
                    lastModifiedBy = createdBy,
                    lastModifiedDate = LocalDateTime.now(),
                    projectId = projectId,
                    repoName = repoName,
                    name = packageName.trim(),
                    key = packageKey.trim(),
                    type = packageType,
                    downloads = 0,
                    versions = 0,
                    versionTag = versionTag.orEmpty(),
                    extension = packageExtension.orEmpty(),
                    description = packageDescription
                )
                try {
                    packageDao.save(tPackage)
                } catch (exception: DuplicateKeyException) {
                    logger.warn("Create package[$tPackage] error: [${exception.message}]")
                    packageDao.findByKey(projectId, repoName, packageKey)!!
                }
            }
        }
    }

    /**
     * 查找包，不存在则抛异常
     */
    private fun checkPackage(projectId: String, repoName: String, packageKey: String): TPackage {
        return packageDao.findByKey(projectId, repoName, packageKey)
            ?: throw ErrorCodeException(ArtifactMessageCode.PACKAGE_NOT_FOUND, packageKey)
    }

    /**
     * 查找版本，不存在则抛异常
     */
    private fun checkPackageVersion(packageId: String, versionName: String): TPackageVersion {
        return packageVersionDao.findByName(packageId, versionName)
            ?: throw ErrorCodeException(ArtifactMessageCode.VERSION_NOT_FOUND, versionName)
    }

    /**
     * 计算语义化版本顺序
     */
    private fun calculateOrdinal(versionName: String): Long {
        return try {
            SemVersion.parse(versionName).ordinal()
        } catch (exception: IllegalArgumentException) {
            LOWEST_ORDINAL
        }
    }

    /**
     * 合并version tag
     */
    private fun mergeVersionTag(
        original: Map<String, String>?,
        extra: Map<String, String>?
    ): Map<String, String> {
        return original?.toMutableMap()?.apply {
            extra?.forEach { (tag, version) -> this[tag] = version }
        }.orEmpty()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PackageServiceImpl::class.java)
        private const val LOWEST_ORDINAL = 0L

        private fun convert(tPackage: TPackage?): PackageSummary? {
            return tPackage?.let {
                PackageSummary(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate,
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate,
                    projectId = it.projectId,
                    repoName = it.repoName,
                    name = it.name,
                    key = it.key,
                    type = it.type,
                    latest = it.latest.orEmpty(),
                    downloads = it.downloads,
                    versions = it.versions,
                    description = it.description,
                    versionTag = it.versionTag.orEmpty(),
                    extension = it.extension.orEmpty()
                )
            }
        }

        private fun convert(tPackageVersion: TPackageVersion?): PackageVersion? {
            return tPackageVersion?.let {
                PackageVersion(
                    createdBy = it.createdBy,
                    createdDate = it.createdDate,
                    lastModifiedBy = it.lastModifiedBy,
                    lastModifiedDate = it.lastModifiedDate,
                    name = it.name,
                    size = it.size,
                    downloads = it.downloads,
                    stageTag = it.stageTag,
                    metadata = MetadataUtils.toMap(it.metadata),
                    tags = it.tags.orEmpty(),
                    extension = it.extension.orEmpty(),
                    contentPath = it.artifactPath
                )
            }
        }
    }
}
