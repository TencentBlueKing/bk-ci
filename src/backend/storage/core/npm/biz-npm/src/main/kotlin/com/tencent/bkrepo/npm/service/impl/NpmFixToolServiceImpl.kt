package com.tencent.bkrepo.npm.service.impl

import com.google.gson.JsonObject
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.exception.PackageNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.DIST
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_METADATA_FULL_PATH
import com.tencent.bkrepo.npm.constants.SIZE
import com.tencent.bkrepo.npm.constants.TIME
import com.tencent.bkrepo.npm.constants.VERSIONS
import com.tencent.bkrepo.npm.handler.NpmPackageHandler
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.pojo.fixtool.DateTimeFormatResponse
import com.tencent.bkrepo.npm.pojo.fixtool.FailPackageDetail
import com.tencent.bkrepo.npm.pojo.fixtool.PackageManagerResponse
import com.tencent.bkrepo.npm.pojo.fixtool.PackageMetadataFixResponse
import com.tencent.bkrepo.npm.service.NpmFixToolService
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class NpmFixToolServiceImpl(
	private val storageManager: StorageManager,
	private val npmPackageHandler: NpmPackageHandler
) : NpmFixToolService, AbstractNpmService() {

	@Permission(ResourceType.REPO, PermissionAction.WRITE)
	override fun fixPackageSizeField(artifactInfo: NpmArtifactInfo): PackageMetadataFixResponse {
		with(artifactInfo) {
			logger.info("starting synchronized npm package metadata in repo [$projectId/$repoName]")
			return repairPackageMetadata(projectId, repoName)
		}
	}

	/**
	 * 修复[projectId]-[repoName]仓库下的npm的package.json数据
	 */
	private fun repairPackageMetadata(projectId: String, repoName: String): PackageMetadataFixResponse {
		var successCount = 0L
		var failedCount = 0L
		var totalCount = 0L
		val startTime = LocalDateTime.now()
		// 分页查询文件节点，以package.json文件为后缀
		var page = 1
		val packageMetadataPage = queryPackageMetadata(projectId, repoName, page)
		var packageMetadataList = packageMetadataPage.records.map { resolveNode(it) }
		if (packageMetadataList.isEmpty()) {
			logger.info("no package found in repo [$projectId/$repoName], return.")
			return PackageMetadataFixResponse(projectId, repoName, successCount, failedCount)
		}
		while (packageMetadataList.isNotEmpty()) {
			packageMetadataList.forEach {
				logger.info(
					"Retrieved ${packageMetadataList.size} records to repair, " +
						"process: $totalCount/${packageMetadataPage.totalRecords}"
				)
				val packageName = it.fullPath.removePrefix("/.npm/").removeSuffix("/package.json")
				try {
					// 修复metadata
					repairNode(projectId, repoName, packageName, it)
					logger.info("Success to repair package [$packageName] in repo [$projectId/$repoName].")
					successCount += 1
				} catch (exception: RuntimeException) {
					logger.error(
						"Failed to to repair package [$packageName] in repo [$projectId/$repoName].",
						exception
					)
					failedCount += 1
				} finally {
					totalCount += 1
				}
			}
			page += 1
			packageMetadataList = queryPackageMetadata(projectId, repoName, page).records.map { resolveNode(it) }
		}
		val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
		logger.info(
			"Repair npm package metadata file in repo [$projectId/$repoName], " +
				"total: $totalCount, success: $successCount, failed: $failedCount, duration $durationSeconds s totally."
		)
		return PackageMetadataFixResponse(projectId, repoName, successCount, failedCount)
	}

	private fun repairNode(projectId: String, repoName: String, packageName: String, nodeInfo: NodeInfo) {
		val successVersionList = mutableListOf<String>()
		val existsAttrVersionList = mutableListOf<String>()
		val packageJson = storageManager.loadArtifactInputStream(nodeInfo, null)
			?.use { GsonUtils.transferInputStreamToJson(it) }
			?: throw IllegalStateException("src package json not found in repo [$projectId/$repoName]")
		val queryTgzNode = queryTgzNode(projectId, repoName, packageName)
		val iterator = packageJson.getAsJsonObject(VERSIONS).entrySet().iterator()
		var isModify = false
		while (iterator.hasNext()) {
			val next = iterator.next()
			val versionDistObject = next.value.asJsonObject.getAsJsonObject(DIST)
			if (!versionDistObject.has(SIZE)) {
				versionDistObject.addProperty(SIZE, queryTgzNode[next.key]?.size)
				isModify = true
				successVersionList.add(next.key)
			} else {
				existsAttrVersionList.add(next.key)
			}
		}
		if (isModify) {
			storePackageArtifact(packageJson, nodeInfo)
			logger.info(
				"repair package metadata for package [$packageName] success " +
					"in repo [$projectId/$repoName], success versions: $successVersionList"
			)
		} else {
			logger.info(
				"the attribute is already exist in package metadata for package $packageName " +
					"with version [$existsAttrVersionList] in repo [$projectId/$repoName], not to be repaired."
			)
		}
	}

	private fun storePackageArtifact(packageJson: JsonObject, nodeInfo: NodeInfo) {
		val artifactFile = ArtifactFileFactory.build(GsonUtils.gsonToInputStream(packageJson))
		with(nodeInfo) {
			val repositoryDetail = checkRepositoryExist(projectId, repoName)
			val request = NodeCreateRequest(
				projectId = projectId,
				repoName = repoName,
				fullPath = fullPath,
				folder = folder,
				overwrite = true,
				size = artifactFile.getSize(),
				sha256 = artifactFile.getFileSha256(),
				md5 = artifactFile.getFileMd5()
			)
			storageManager.storeArtifactFile(request, artifactFile, repositoryDetail.storageCredentials)
			artifactFile.delete()
		}
	}

	@Permission(ResourceType.REPO, PermissionAction.WRITE)
	override fun fixDateFormat(artifactInfo: NpmArtifactInfo, pkgName: String): DateTimeFormatResponse {
		val pkgNameSet = pkgName.split(',').filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
		logger.info("fix time format with package: $pkgNameSet, size : [${pkgNameSet.size}]")
		val successSet = mutableSetOf<String>()
		val errorSet = mutableSetOf<String>()
		val context = ArtifactQueryContext()
		val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
		pkgNameSet.forEach {
			try {
				val fullPath = String.format(NPM_PKG_METADATA_FULL_PATH, it)
				context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
				val pkgFileInfo = repository.query(context) as? JsonObject
				if (pkgFileInfo == null) {
					errorSet.add(it)
					return@forEach
				}
				repairTimeFormat(pkgFileInfo)
				reUploadPkgJson(pkgFileInfo)
				successSet.add(it)
			} catch (ignored: Exception) {
				errorSet.add(it)
			}
		}
		return DateTimeFormatResponse(successSet, errorSet)
	}

	private fun repairTimeFormat(pkgFileInfo: JsonObject) {
		val timeJsonObject = pkgFileInfo[TIME].asJsonObject
		timeJsonObject.entrySet().forEach {
			if (!it.value.asString.contains('T')) {
				timeJsonObject.add(it.key, GsonUtils.gson.toJsonTree(formatDateTime(it.value.asString)))
			}
		}
	}

	private fun reUploadPkgJson(pkgFileInfo: JsonObject) {
		val name = pkgFileInfo[NAME].asString
		val pkgMetadata = ArtifactFileFactory.build(GsonUtils.gsonToInputStream(pkgFileInfo))
		val context = ArtifactUploadContext(pkgMetadata)
		val fullPath = String.format(NPM_PKG_METADATA_FULL_PATH, name)
		context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
		val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
		repository.upload(context)
	}

	fun formatDateTime(time: String): String {
		val dateFormat = "yyyy-MM-dd HH:mm:ss.SSS'Z'"
		val dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern(dateFormat))
		return TimeUtil.getGMTTime(dateTime)
	}

	/**
	 * 历史数据增加package功能
	 */
	override fun fixPackageManager(): List<PackageManagerResponse> {
		val packageManagerList = mutableListOf<PackageManagerResponse>()
		// 查找所有仓库
		logger.info("starting add package manager function to historical data.")
		val repositoryList = repositoryClient.pageByType(0, 1000, "NPM").data?.records ?: run {
			logger.warn("no npm repository found, return.")
			return emptyList()
		}
		val npmLocalRepositoryList = repositoryList.filter { it.category == RepositoryCategory.LOCAL }.toList()
		logger.info(
			"find [${npmLocalRepositoryList.size}] NPM local" +
				" repository ${repositoryList.map { it.projectId to it.name }}"
		)
		npmLocalRepositoryList.forEach {
			val packageManagerResponse = addPackageManager(it.projectId, it.name)
			packageManagerList.add(packageManagerResponse)
		}
		return packageManagerList
	}

	private fun addPackageManager(projectId: String, repoName: String): PackageManagerResponse {
		// 查询仓库下面的所有package的包
		var successCount = 0L
		var failedCount = 0L
		var totalCount = 0L
		val failedSet = mutableSetOf<FailPackageDetail>()
		val startTime = LocalDateTime.now()

		// 分页查询文件节点，以package.json文件为后缀
		var page = 1
		val packageMetadataPage = queryPackageMetadata(projectId, repoName, page)
		var packageMetadataList = packageMetadataPage.records.map { resolveNode(it) }
		if (packageMetadataList.isEmpty()) {
			logger.info("no package found in repo [$projectId/$repoName], skip.")
			return PackageManagerResponse.emptyResponse(projectId, repoName)
		}
		while (packageMetadataList.isNotEmpty()) {
			packageMetadataList.forEach {
				logger.info(
					"Retrieved ${packageMetadataPage.totalRecords} records to add package manager " +
						"in repo [$projectId/$repoName], process: $totalCount/${packageMetadataPage.totalRecords}"
				)
				val packageName = it.fullPath.removePrefix("/.npm/").removeSuffix("/package.json")
				var failPackageDetail: FailPackageDetail? = null
				try {
					// 添加包管理
					failPackageDetail = doAddPackageManager(projectId, repoName, packageName, it)
//                    failPackageDetail?.let { failedSet.add(it) }
					logger.info("Success to add package manager for [$packageName] in repo [$projectId/$repoName].")
					successCount += 1
				} catch (exception: RuntimeException) {
					logger.error(
						"Failed to add package manager for [$packageName] in repo [$projectId/$repoName].",
						exception
					)
//                    failedSet.add(FailPackageDetail(packageName, mutableSetOf()))
					failedSet.add(failPackageDetail!!)
					failedCount += 1
				} finally {
					totalCount += 1
				}
			}
			page += 1
			packageMetadataList = queryPackageMetadata(projectId, repoName, page).records.map { resolveNode(it) }
		}
		val durationSeconds = Duration.between(startTime, LocalDateTime.now()).seconds
		logger.info(
			"Repair npm package metadata file in repo [$projectId/$repoName], " +
				"total: $totalCount, success: $successCount, failed: $failedCount, " +
				"duration $durationSeconds s totally."
		)
		return PackageManagerResponse(
			projectId = projectId,
			repoName = repoName,
			totalCount = totalCount,
			successCount = successCount,
			failedCount = failedCount,
			failedSet = failedSet
		)
	}

	private fun doAddPackageManager(
		projectId: String,
		repoName: String,
		packageName: String,
		nodeInfo: NodeInfo
	): FailPackageDetail? {
		val packageMetaData = storageManager.loadArtifactInputStream(nodeInfo, null)
			?.use { JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java) }
			?: throw IllegalStateException("src package json not found in repo [$projectId/$repoName]")
		val tgzNodeInfoMap = queryTgzNode(projectId, repoName, packageName)
		return npmPackageHandler.populatePackage(nodeInfo, packageMetaData, tgzNodeInfoMap).also {
			logger.info("add package manager for package [$packageName] success in repo [$projectId/$repoName]")
		}
	}

	private fun queryTgzNode(
		projectId: String,
		repoName: String,
		packageName: String
	): Map<String, NodeInfo> {
		val ruleList = mutableListOf<Rule>(
			Rule.QueryRule("projectId", projectId, OperationType.EQ),
			Rule.QueryRule("repoName", repoName, OperationType.EQ),
			Rule.QueryRule("folder", false, OperationType.EQ),
			Rule.QueryRule("fullPath", "/$packageName", OperationType.PREFIX),
			Rule.QueryRule("fullPath", "tgz", OperationType.SUFFIX)
		)
		val queryModel = QueryModel(
			page = PageLimit(0, 10000),
			sort = Sort(listOf("lastModifiedDate"), Sort.Direction.ASC),
			select = mutableListOf(),
			rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
		)
		val queryResult = nodeClient.search(queryModel).data!!
		return queryResult.records.associateBy(
			{ resolverVersion(packageName, it["fullPath"] as String) },
			{ resolveNode(it) }
		)
	}

	private fun resolverVersion(packageName: String, fullPath: String): String {
		val tgzFileName = fullPath.split('/').last()
		val last = packageName.split('/').last()
		return tgzFileName.removePrefix("$last-").removeSuffix(".tgz")
	}

	private fun queryPackageMetadata(
		projectId: String,
		repoName: String,
		page: Int
	): Page<Map<String, Any?>> {
		val ruleList = mutableListOf<Rule>(
			Rule.QueryRule("projectId", projectId, OperationType.EQ),
			Rule.QueryRule("repoName", repoName, OperationType.EQ),
			Rule.QueryRule("folder", false, OperationType.EQ),
			Rule.QueryRule("name", "package.json", OperationType.EQ)
		)
		val queryModel = QueryModel(
			page = PageLimit(page, pageSize),
			sort = null,
			select = mutableListOf(),
			rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)
		)
		return nodeClient.search(queryModel).data!!
	}

	private fun resolveNode(record: Map<String, Any?>): NodeInfo {
		return NodeInfo(
			createdBy = record["createdBy"] as String,
			createdDate = record["createdDate"] as String,
			lastModifiedBy = record["lastModifiedBy"] as String,
			lastModifiedDate = record["lastModifiedDate"] as String,
			folder = record["folder"] as Boolean,
			path = record["path"] as String,
			name = record["name"] as String,
			fullPath = record["fullPath"] as String,
			size = record["size"].toString().toLong(),
			sha256 = record["sha256"] as String,
			md5 = record["md5"] as String,
			projectId = record["projectId"] as String,
			repoName = record["repoName"] as String,
			metadata = mapOf()
		)
	}

	@Permission(ResourceType.REPO, PermissionAction.WRITE)
	override fun inconsistentCorrectionData(artifactInfo: NpmArtifactInfo, name: String) {
		/**
		 * 查找到包的package.json文件，找到所有的版本
		 * 查找package的所有版本
		 * 对比版本差异，找到不存在的所有版本，删除即可，也要删除对饮的版本json文件
		 * 处理时注意tag处理，如果tag关联的版本不存在，删除该tag，如果是latest版本删除了则需要重新找个最新版本为latest版本
		 */
		with(artifactInfo) {
			val packageKey = PackageKeys.ofNpm(name)
			val packageSummary = packageClient.findPackageByKey(projectId, repoName, packageKey).data
				?: throw PackageNotFoundException("package [$name] not found")
			val packageInfo = queryPackageInfo(this, name, false)
			val metadataVersions = packageInfo.versions.map.keys
			val packageVersions =
				packageClient.listAllVersion(projectId, repoName, packageKey).data.orEmpty().map { it.name }
			val subVersion = metadataVersions subtract packageVersions
			val fullPathList = mutableListOf<String>()
			if (subVersion.isNotEmpty()) {
				// 处理tag
				handlerDistTag(packageInfo, subVersion, packageSummary)
				// packageInfo
				subVersion.forEach {
					// 删除版本索引
					packageInfo.versions.map.remove(it)
					// 删除时间
					packageInfo.time.getMap().remove(it)
					// 删除版本索引文件
					fullPathList.add(NpmUtils.getVersionPackageMetadataPath(name, it))
				}
				removePackageVersionMetadata(fullPathList)
				// 重新上传json索引文件
				uploadPackageMetadata(packageInfo)
			}
		}
	}

	private fun handlerDistTag(
		packageInfo: NpmPackageMetaData,
		subVersion: Set<String>,
		packageSummary: PackageSummary
	) {
		val tagMap = packageInfo.distTags.getMap()
		val iterator = tagMap.entries.iterator()
		// 移除metadata中的distTag不存在的版本标签
		while (iterator.hasNext()) {
			val entry = iterator.next()
			if (subVersion.contains(entry.value)) {
				iterator.remove()
			}
		}
		// 如果latest版本也移除了，则获取当前的package中的最新版本作为latest版本
		if (!tagMap.containsKey(LATEST)) {
			tagMap[LATEST] = packageSummary.latest
		}
	}

	private fun removePackageVersionMetadata(fullPathList: List<String>) {
		val context = ArtifactRemoveContext()
		context.putAttribute(NPM_FILE_FULL_PATH, fullPathList)
		val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
		repository.remove(context)
	}

	private fun uploadPackageMetadata(packageInfo: NpmPackageMetaData) {
		val name = packageInfo.name.orEmpty()
		val artifactFile = JsonUtils.objectMapper.writeValueAsBytes(packageInfo).inputStream()
			.use { ArtifactFileFactory.build(it) }
		val context = ArtifactUploadContext(artifactFile)
		val fullPath = String.format(NPM_PKG_METADATA_FULL_PATH, name)
		context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
		val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
		repository.upload(context)
	}

	companion object {
		private const val pageSize = 10000
		private val logger = LoggerFactory.getLogger(NpmFixToolServiceImpl::class.java)
	}
}
