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

package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_TGZ_TARBALL_PREFIX
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.exception.NpmRepoNotFoundException
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.properties.NpmProperties
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

// LateinitUsage: 抽象类中使用构造器注入会造成不便
@Suppress("LateinitUsage")
open class AbstractNpmService {

	@Autowired
	lateinit var nodeClient: NodeClient

	@Autowired
	lateinit var repositoryClient: RepositoryClient

	@Autowired
	lateinit var packageClient: PackageClient

	@Autowired
	lateinit var npmProperties: NpmProperties

	/**
	 * 查询仓库是否存在
	 */
	fun checkRepositoryExist(projectId: String, repoName: String): RepositoryDetail {
		return repositoryClient.getRepoDetail(projectId, repoName, "NPM").data ?: run {
			logger.error("check repository [$repoName] in projectId [$projectId] failed!")
			throw NpmRepoNotFoundException("repository [$repoName] in projectId [$projectId] not existed.")
		}
	}

	/**
	 * check package exists
	 */
	fun packageExist(projectId: String, repoName: String, key: String): Boolean {
		return packageClient.findPackageByKey(projectId, repoName, key).data?.let { true } ?: false
	}

	/**
	 * check package version exists
	 */
	fun packageVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
		return packageClient.findVersionByName(projectId, repoName, key, version).data?.let { true } ?: false
	}

	/**
	 * check package history version exists
	 */
	fun packageHistoryVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
		val packageSummary = packageClient.findPackageByKey(projectId, repoName, key).data ?: return false
		return packageSummary.historyVersion.contains(version)
	}

	/**
	 * query package metadata
	 */
	fun queryPackageInfo(
		artifactInfo: NpmArtifactInfo,
		name: String,
		showCustomTarball: Boolean = true
	): NpmPackageMetaData {
		val packageFullPath = NpmUtils.getPackageMetadataPath(name)
		val context = ArtifactQueryContext()
		context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
		val inputStream =
			ArtifactContextHolder.getRepository().query(context) as? InputStream
				?: throw NpmArtifactNotFoundException("document not found")
		val packageMetaData = inputStream.use { JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java) }
		if (showCustomTarball && !showDefaultTarball()) {
			val versionsMap = packageMetaData.versions.map
			val iterator = versionsMap.entries.iterator()
			while (iterator.hasNext()) {
				val entry = iterator.next()
				modifyVersionMetadataTarball(artifactInfo, name, entry.value)
			}
		}
		return packageMetaData
	}

	private fun showDefaultTarball(): Boolean {
		val domain = npmProperties.domain
		val tarballPrefix = npmProperties.tarball.prefix
		val npmPrefixHeader = HeaderUtils.getHeader(NPM_TGZ_TARBALL_PREFIX).orEmpty()
		return npmPrefixHeader.isEmpty() && tarballPrefix.isEmpty() && domain.isEmpty()
	}

	protected fun modifyVersionMetadataTarball(
		artifactInfo: NpmArtifactInfo,
		name: String,
		versionMetadata: NpmVersionMetadata
	) {
		val oldTarball = versionMetadata.dist?.tarball!!
		versionMetadata.dist?.tarball =
			NpmUtils.buildPackageTgzTarball(
				oldTarball, npmProperties.domain, npmProperties.tarball.prefix, name, artifactInfo
			)
	}

	companion object {
		val logger: Logger = LoggerFactory.getLogger(AbstractNpmService::class.java)
	}
}
