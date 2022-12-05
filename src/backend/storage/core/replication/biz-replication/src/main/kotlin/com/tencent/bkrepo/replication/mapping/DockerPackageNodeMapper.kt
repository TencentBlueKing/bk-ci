package com.tencent.bkrepo.replication.mapping

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Component

/**
 * DOCKER 依赖源需要迁移manifest.json文件以及该文件内容里面包含的config文件和layers文件
 */
@Component
class DockerPackageNodeMapper(
    private val nodeClient: NodeClient,
    private val storageService: StorageService,
    private val repositoryClient: RepositoryClient
) : PackageNodeMapper {

    override fun type() = RepositoryType.DOCKER

    override fun map(
        packageSummary: PackageSummary,
        packageVersion: PackageVersion,
        type: RepositoryType
    ): List<String> {
        with(packageSummary) {
            val result = mutableListOf<String>()
            val name = PackageKeys.resolveDocker(key)
            val version = packageVersion.name
            val manifestFullPath = DOCKER_MANIFEST_JSON_FULL_PATH.format(name, version)
            val repository = repositoryClient.getRepoDetail(projectId, repoName, type.name).data!!
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, manifestFullPath).data!!
            val inputStream = storageService.load(
                nodeDetail.sha256.orEmpty(),
                Range.full(nodeDetail.size),
                repository.storageCredentials
            )!!
            val layersList = parseManifest(inputStream)
            layersList.forEach {
                val replace = it.replace(":", "__")
                result.add(DOCKER_LAYER_FULL_PATH.format(name, version, replace))
            }
            result.add(DOCKER_MANIFEST_JSON_FULL_PATH.format(name, version))
            return result
        }
    }

    private fun parseManifest(inputStream: ArtifactInputStream): List<String> {
        val list = mutableListOf<String>()
        val manifest = inputStream.use { it.readJsonString<Manifest>() }
        val configFullPath = manifest.config.digest
        val iterator = manifest.layers.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            list.add(next.digest)
        }
        list.add(configFullPath)
        return list
    }

    companion object {
        const val DOCKER_MANIFEST_JSON_FULL_PATH = "/%s/%s/manifest.json"
        const val DOCKER_LAYER_FULL_PATH = "/%s/%s/%s"
    }

    data class Manifest(
        val schemaVersion: Int,
        val mediaType: String,
        val config: Layer,
        val layers: List<Layer>
    )

    data class Layer(
        val mediaType: String,
        val size: Long,
        val digest: String
    )
}
