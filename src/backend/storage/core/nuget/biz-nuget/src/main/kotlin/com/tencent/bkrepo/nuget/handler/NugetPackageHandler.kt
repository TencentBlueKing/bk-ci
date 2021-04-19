package com.tencent.bkrepo.nuget.handler

import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.nuspec.Dependency
import com.tencent.bkrepo.nuget.model.nuspec.DependencyGroup
import com.tencent.bkrepo.nuget.model.nuspec.FrameworkAssembly
import com.tencent.bkrepo.nuget.model.nuspec.NuspecMetadata
import com.tencent.bkrepo.nuget.model.nuspec.Reference
import com.tencent.bkrepo.nuget.model.nuspec.ReferenceGroup
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.StringJoiner
import kotlin.system.measureTimeMillis

@Component
class NugetPackageHandler {

    @Autowired
    private lateinit var packageClient: PackageClient

    /**
     * 创建包版本
     */
    fun createPackageVersion(
        userId: String,
        artifactInfo: NugetArtifactInfo,
        nuspecMetadata: NuspecMetadata,
        size: Long
    ) {
        nuspecMetadata.apply {
            var metadata: Map<String, Any>? = null
            logger.info(
                "start index nuget metadata for package [$id] and version [$version] " +
                    "in repo [${artifactInfo.getRepoIdentify()}]"
            )
            measureTimeMillis { metadata = indexMetadata(this) }.apply {
                logger.info(
                    "finished index nuget metadata for package [$id] and version [$version] " +
                        "in repo [${artifactInfo.getRepoIdentify()}], elapse [$this] ms."
                )
            }
            with(artifactInfo) {
                val packageVersionCreateRequest = PackageVersionCreateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageName = id,
                    packageKey = PackageKeys.ofNuget(id.toLowerCase()),
                    packageType = PackageType.NUGET,
                    packageDescription = description,
                    versionName = version,
                    size = size,
                    manifestPath = null,
                    artifactPath = getContentPath(id, version),
                    stageTag = null,
                    metadata = metadata,
                    overwrite = true,
                    createdBy = userId
                )
                packageClient.createVersion(packageVersionCreateRequest).apply {
                    logger.info("user: [$userId] create package version [$packageVersionCreateRequest] success!")
                }
            }
        }
    }

    private fun indexMetadata(nuspecMetadata: NuspecMetadata): Map<String, Any> {
        val metadata: MutableMap<String, Any> = mutableMapOf()
        if (nuspecMetadata.isValid()) {
            with(nuspecMetadata) {
                metadata["id"] = id
                /*metadata["version"] = version
                metadata["title"] = title ?: StringPool.EMPTY
                metadata["authors"] = authors
                metadata["summary"] = summary ?: StringPool.EMPTY
                metadata["copyright"] = copyright ?: StringPool.EMPTY
                metadata["releaseNotes"] = releaseNotes ?: StringPool.EMPTY
                metadata["owners"] = owners ?: StringPool.EMPTY
                metadata["description"] = description
                metadata["requireLicenseAcceptance"] = requireLicenseAcceptance ?: false
                metadata["projectUrl"] = projectUrl ?: StringPool.EMPTY
                metadata["iconUrl"] = iconUrl ?: StringPool.EMPTY
                metadata["icon"] = icon ?: StringPool.EMPTY
                metadata["licenseUrl"] = licenseUrl ?: StringPool.EMPTY
                metadata["tags"] = tags ?: StringPool.EMPTY
                metadata["language"] = language ?: StringPool.EMPTY*/
                metadata["dependency"] = buildDependencies(dependencies)
                metadata["reference"] = buildReferences(references)
                metadata["frameworks"] = buildFrameworks(frameworkAssemblies)
            }
        }
        return metadata
    }

    /**
     * 构造Frameworks
     */
    private fun buildFrameworks(frameworkAssemblies: MutableList<FrameworkAssembly>?): Set<String> {
        if (frameworkAssemblies != null && frameworkAssemblies.isNotEmpty()) {
            val values = hashSetOf<String>()
            val iterator = frameworkAssemblies.iterator()
            while (iterator.hasNext()) {
                val frameworkAssembly = iterator.next()
                values.add(getFrameworkValue(frameworkAssembly))
            }
            return values
        }
        return emptySet()
    }

    fun getFrameworkValue(frameworkAssembly: FrameworkAssembly): String {
        with(frameworkAssembly) {
            return targetFramework?.let {
                StringJoiner(":").add(assemblyName).add(it).toString()
            } ?: assemblyName
        }
    }

    /**
     * 构造references
     */
    private fun buildReferences(references: MutableList<Any>?): Set<String> {
        if (references != null && references.isNotEmpty()) {
            val values = hashSetOf<String>()
            val iterator = references.iterator()
            while (iterator.hasNext()) {
                val reference = iterator.next()
                if (reference is Reference) {
                    values.add(reference.file)
                } else if (reference is ReferenceGroup) {
                    buildReferenceGroup(reference, values)
                }
            }
            return values
        }
        return emptySet()
    }

    private fun buildReferenceGroup(reference: ReferenceGroup, values: HashSet<String>) {
        val groupReferences = reference.references
        groupReferences?.let {
            val groupIterator = it.iterator()
            while (groupIterator.hasNext()) {
                val groupReference = groupIterator.next()
                values.add(groupReference.file)
            }
        }
    }

    /**
     * 构造dependencies
     */
    private fun buildDependencies(dependencies: List<Any>?): Set<String> {
        if (dependencies != null && dependencies.isNotEmpty()) {
            val values = hashSetOf<String>()
            val iterator = dependencies.iterator()
            while (iterator.hasNext()) {
                val dependency = iterator.next()
                if (dependency is Dependency) {
                    values.add(getDependencyValue(dependency, ""))
                } else if (dependency is DependencyGroup) {
                    buildDependencyGroup(dependency, values)
                }
            }
            return values
        }
        return emptySet()
    }

    private fun buildDependencyGroup(dependency: DependencyGroup, values: HashSet<String>) {
        val groupDependencies = dependency.dependencies
        groupDependencies?.let {
            val groupIterator = it.iterator()
            while (groupIterator.hasNext()) {
                val groupDependency = groupIterator.next()
                values.add(getDependencyValue(groupDependency, dependency.targetFramework))
            }
        } ?: values.add("::${dependency.targetFramework}")
    }

    fun getDependencyValue(dependency: Dependency, targetFramework: String): String {
        return StringJoiner(":").add(dependency.id).add(dependency.version).add(targetFramework).toString()
    }

    fun getContentPath(name: String, version: String): String {
        return NugetUtils.getNupkgFileName(name, version)
    }

    // 删除包版本
    fun deleteVersion(userId: String, name: String, version: String, artifactInfo: NugetArtifactInfo) {
        val packageKey = PackageKeys.ofNuget(name)
        with(artifactInfo) {
            packageClient.deleteVersion(projectId, repoName, packageKey, version).apply {
                logger.info(
                    "user: [$userId] delete package [$name] with version [$version] " +
                        "in repo [${artifactInfo.getRepoIdentify()}] success!"
                )
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NugetPackageHandler::class.java)
    }
}
