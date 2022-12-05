package com.tencent.bkrepo.nuget.handler

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.constant.DEPENDENCY
import com.tencent.bkrepo.nuget.constant.FRAMEWORKS
import com.tencent.bkrepo.nuget.constant.ID
import com.tencent.bkrepo.nuget.constant.PACKAGE
import com.tencent.bkrepo.nuget.constant.REFERENCE
import com.tencent.bkrepo.nuget.constant.VERSION
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo
import com.tencent.bkrepo.nuget.pojo.nuspec.Dependency
import com.tencent.bkrepo.nuget.pojo.nuspec.DependencyGroup
import com.tencent.bkrepo.nuget.pojo.nuspec.FrameworkAssembly
import com.tencent.bkrepo.nuget.pojo.nuspec.Reference
import com.tencent.bkrepo.nuget.pojo.nuspec.ReferenceGroup
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.StringJoiner
import kotlin.collections.HashSet

@Component
class NugetPackageHandler {

    @Autowired
    private lateinit var packageClient: PackageClient

    /**
     * 创建包版本
     */
    fun createPackageVersion(
        context: ArtifactUploadContext
    ) {
        with(context.artifactInfo as NugetPublishArtifactInfo) {
            nuspecPackage.metadata.apply {
                logger.info(
                    "start index nuget metadata for package [$id] and version [$version] " +
                        "in repo [${getRepoIdentify()}]"
                )
                val metadata = mutableMapOf<String, Any>()
                metadata[ID] = id
                metadata[VERSION] = version
                dependencies?.let { metadata[DEPENDENCY] = buildDependencies(it) }
                references?.let { metadata[REFERENCE] = buildReferences(it) }
                frameworkAssemblies?.let { metadata[FRAMEWORKS] = buildFrameworks(it) }

//                measureTimeMillis { metadata = indexMetadata(this) }.apply {
//                    logger.info(
//                        "finished index nuget metadata for package [$id] and version [$version] " +
//                            "in repo [${getRepoIdentify()}], elapse [$this] ms."
//                    )
//                }
                // versionExtension
                val versionExtension = mutableMapOf<String, Any>(
                    PACKAGE to this.toJsonString()
                )
                val packageVersionCreateRequest = PackageVersionCreateRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageName = id,
                    packageKey = PackageKeys.ofNuget(id.toLowerCase()),
                    packageType = PackageType.NUGET,
                    packageDescription = description,
                    versionName = version,
                    size = size,
                    artifactPath = getArtifactFullPath(),
                    metadata = metadata,
                    extension = versionExtension,
                    overwrite = true,
                    createdBy = context.userId
                )
                packageClient.createVersion(packageVersionCreateRequest, HttpContextHolder.getClientAddress())
                if (logger.isDebugEnabled) {
                    logger.info(
                        "user: [${context.userId}] create package version [$packageVersionCreateRequest] success!"
                    )
                }
            }
        }
    }

    /**private fun indexMetadata(nuspecMetadata: NuspecMetadata): Map<String, Any> {
    val metadata: MutableMap<String, Any> = mutableMapOf()
    if (nuspecMetadata.isValid()) {
    with(nuspecMetadata) {
    metadata["id"] = id
    metadata["version"] = version
    metadata["authors"] = authors
    metadata["description"] = description
    owners?.let { metadata["owners"] = it }
    projectUrl?.let { metadata["projectUrl"] = it }
    licenseUrl?.let { metadata["licenseUrl"] = it }
    license?.let { metadata["license"] = it }
    iconUrl?.let { metadata["iconUrl"] = it }
    icon?.let { metadata["icon"] = it }
    requireLicenseAcceptance?.let { metadata["requireLicenseAcceptance"] = it }
    developmentDependency?.let { metadata["developmentDependency"] = it }
    summary?.let { metadata["summary"] = it }
    releaseNotes?.let { metadata["releaseNotes"] = it }
    copyright?.let { metadata["copyright"] = it }
    language?.let { metadata["language"] = it }
    tags?.let { metadata["tags"] = it }
    serviceable?.let { metadata["serviceable"] = it }
    title?.let { metadata["title"] = it }
    dependencies?.let { metadata["dependency"] = buildDependencies(it) }
    references?.let { metadata["reference"] = buildReferences(it) }
    frameworkAssemblies?.let { metadata["frameworks"] = buildFrameworks(it) }
    }
    }
    return metadata
    }**/

    /**
     * 构造Frameworks
     */
    private fun buildFrameworks(frameworkAssemblies: MutableList<FrameworkAssembly>): Set<String> {
        if (frameworkAssemblies.isNotEmpty()) {
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
    private fun buildReferences(references: MutableList<Any>): Set<String> {
        if (references.isNotEmpty()) {
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
    private fun buildDependencies(dependencies: List<Any>): Set<String> {
        if (dependencies.isNotEmpty()) {
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

    // 删除包版本
    fun deleteVersion(userId: String, name: String, version: String, artifactInfo: NugetArtifactInfo) {
        val packageKey = PackageKeys.ofNuget(name)
        with(artifactInfo) {
            packageClient.deleteVersion(projectId, repoName, packageKey, version, HttpContextHolder.getClientAddress())
                .apply {
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
