package com.tencent.bkrepo.maven.service

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.maven.dao.MavenMetadataDao
import com.tencent.bkrepo.maven.model.TMavenMetadataRecord
import com.tencent.bkrepo.maven.pojo.MavenGAVC
import com.tencent.bkrepo.maven.pojo.MavenMetadataSearchPojo
import com.tencent.bkrepo.maven.pojo.MavenVersion
import com.tencent.bkrepo.maven.util.MavenStringUtils.resolverName
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

@Service
class MavenMetadataService(
    private val mavenMetadataDao: MavenMetadataDao
) {
    fun update(node: NodeCreateRequest) {
        val (criteria, mavenVersion) = nodeCriteria(
            projectId = node.projectId,
            repoName = node.repoName,
            metadata = node.metadata,
            fullPath = node.fullPath
        )
        if (criteria == null || mavenVersion == null) return
        val query = Query(criteria)
        val update = Update().set(TMavenMetadataRecord::timestamp.name, mavenVersion.timestamp)
            .set(TMavenMetadataRecord::buildNo.name, mavenVersion.buildNo ?: 0)
        val options = FindAndModifyOptions().apply { this.upsert(true).returnNew(false) }
        val returnData = mavenMetadataDao.determineMongoTemplate()
            .findAndModify(query, update, options, TMavenMetadataRecord::class.java)
        returnData?.let {
            logger.info(
                "Old meta data info: extension[${returnData.extension}]," +
                    " groupId[${returnData.groupId}], " +
                    " artifactId[${returnData.artifactId}], " +
                    " version[${returnData.version}]" +
                    " classifier[${returnData.classifier}]," +
                    " timestamp[${returnData.timestamp}]"
            )
        }
    }

    private fun nodeCriteria(
        projectId: String,
        repoName: String,
        metadata: Map<String, Any>? = null,
        fullPath: String
    ): Pair<Criteria?, MavenVersion?> {
        if (validateMetaData(metadata)) return Pair(null, null)
        val groupId = metadata?.get("groupId") as String
        val artifactId = metadata["artifactId"] as String
        val version = metadata["version"] as String
        logger.info(
            "Node info: groupId[$groupId], artifactId[$artifactId], version[$version], Node fullPath: $fullPath"
        )
        val criteria = Criteria.where(TMavenMetadataRecord::projectId.name).`is`(projectId)
            .and(TMavenMetadataRecord::repoName.name).`is`(repoName)
            .and(TMavenMetadataRecord::groupId.name).`is`(groupId)
            .and(TMavenMetadataRecord::artifactId.name).`is`(artifactId)
            .and(TMavenMetadataRecord::version.name).`is`(version)
        val mavenVersion =
            fullPath.substringAfterLast("/").resolverName(artifactId, version)
        criteria.and(TMavenMetadataRecord::extension.name).`is`(mavenVersion.packaging)
        if (mavenVersion.classifier == null) {
            criteria.and(TMavenMetadataRecord::classifier.name).exists(false)
        } else {
            criteria.and(TMavenMetadataRecord::classifier.name).`is`(mavenVersion.classifier)
        }
        logger.info(
            "Node info: extension[${mavenVersion.packaging}]," +
                " classifier[${mavenVersion.classifier}], buildNo[${mavenVersion.buildNo}]" +
                " timestamp[${mavenVersion.timestamp}] , fullPath: $fullPath"
        )
        return Pair(criteria, mavenVersion)
    }

    private fun validateMetaData(metadata: Map<String, Any>? = null): Boolean {
        if (metadata.isNullOrEmpty()) return true
        return (
            metadata["groupId"] == null ||
                metadata["artifactId"] == null ||
                metadata["version"] == null
            )
    }

    fun delete(mavenArtifactInfo: ArtifactInfo, node: NodeDetail? = null, mavenGavc: MavenGAVC? = null) {
        node?.let {
            val (criteria, _) = nodeCriteria(
                projectId = node.projectId,
                repoName = node.repoName,
                metadata = node.metadata,
                fullPath = node.fullPath
            )
            criteria?.let {
                val query = Query(criteria)
                mavenMetadataDao.remove(query)
            }
        }
        mavenGavc?.let {
            val groupId = mavenGavc.groupId
            val artifactId = mavenGavc.artifactId
            val version = mavenGavc.version
            logger.info(
                "Node info: groupId[$groupId], artifactId[$artifactId], version[$version]"
            )
            val criteria = Criteria.where(TMavenMetadataRecord::projectId.name).`is`(mavenArtifactInfo.projectId)
                .and(TMavenMetadataRecord::repoName.name).`is`(mavenArtifactInfo.repoName)
                .and(TMavenMetadataRecord::groupId.name).`is`(groupId)
                .and(TMavenMetadataRecord::artifactId.name).`is`(artifactId)
                .and(TMavenMetadataRecord::version.name).`is`(version)
            val query = Query(criteria)
            mavenMetadataDao.remove(query)
        }
    }

    fun search(mavenArtifactInfo: ArtifactInfo, mavenGavc: MavenGAVC): List<TMavenMetadataRecord> {
        logger.info(
            "Searching Node info: groupId[${mavenGavc.groupId}], artifactId[${mavenGavc.artifactId}], " +
                "version[${mavenGavc.version}], repoName: ${mavenArtifactInfo.repoName}, " +
                "projectId[${mavenArtifactInfo.projectId}]"
        )
        val criteria = Criteria.where(TMavenMetadataRecord::projectId.name).`is`(mavenArtifactInfo.projectId)
            .and(TMavenMetadataRecord::repoName.name).`is`(mavenArtifactInfo.repoName)
            .and(TMavenMetadataRecord::groupId.name).`is`(mavenGavc.groupId)
            .and(TMavenMetadataRecord::artifactId.name).`is`(mavenGavc.artifactId)
            .and(TMavenMetadataRecord::version.name).`is`(mavenGavc.version)
        val query = Query(criteria)
        return mavenMetadataDao.find(query)
    }

    fun findAndModify(mavenMetadataSearchPojo: MavenMetadataSearchPojo): TMavenMetadataRecord {
        logger.info(
            "findAndModify metadata groupId[${mavenMetadataSearchPojo.groupId}], " +
                "artifactId[${mavenMetadataSearchPojo.artifactId}], " +
                "version[${mavenMetadataSearchPojo.version}]," +
                "extension[${mavenMetadataSearchPojo.extension}]," +
                "classifier[${mavenMetadataSearchPojo.classifier}]"
        )
        val criteria = Criteria.where(TMavenMetadataRecord::projectId.name).`is`(mavenMetadataSearchPojo.projectId)
            .and(TMavenMetadataRecord::repoName.name).`is`(mavenMetadataSearchPojo.repoName)
            .and(TMavenMetadataRecord::groupId.name).`is`(mavenMetadataSearchPojo.groupId)
            .and(TMavenMetadataRecord::artifactId.name).`is`(mavenMetadataSearchPojo.artifactId)
            .and(TMavenMetadataRecord::version.name).`is`(mavenMetadataSearchPojo.version)
            .and(TMavenMetadataRecord::extension.name).`is`(mavenMetadataSearchPojo.extension)
        if (mavenMetadataSearchPojo.classifier == null) {
            criteria.and(TMavenMetadataRecord::classifier.name).exists(false)
        } else {
            criteria.and(TMavenMetadataRecord::classifier.name).`is`(mavenMetadataSearchPojo.classifier)
        }
        val query = Query(criteria)
        val update = Update().apply {
            this.set(TMavenMetadataRecord::timestamp.name, ZonedDateTime.now(ZoneId.of("UTC")).format(formatter))
                .inc(TMavenMetadataRecord::buildNo.name)
        }
        val options = FindAndModifyOptions().upsert(true).returnNew(true)
        return mavenMetadataDao.determineMongoTemplate()
            .findAndModify(query, update, options, TMavenMetadataRecord::class.java)!!
    }

    fun search(mavenMetadataSearchPojo: MavenMetadataSearchPojo): List<TMavenMetadataRecord>? {
        logger.info(
            "search metadata groupId[${mavenMetadataSearchPojo.groupId}], " +
                "artifactId[${mavenMetadataSearchPojo.artifactId}], " +
                "version[${mavenMetadataSearchPojo.version}]," +
                "extension[${mavenMetadataSearchPojo.extension}]," +
                "classifier[${mavenMetadataSearchPojo.classifier}]"
        )
        val criteria = Criteria.where(TMavenMetadataRecord::projectId.name).`is`(mavenMetadataSearchPojo.projectId)
            .and(TMavenMetadataRecord::repoName.name).`is`(mavenMetadataSearchPojo.repoName)
            .and(TMavenMetadataRecord::groupId.name).`is`(mavenMetadataSearchPojo.groupId)
            .and(TMavenMetadataRecord::artifactId.name).`is`(mavenMetadataSearchPojo.artifactId)
            .and(TMavenMetadataRecord::version.name).`is`(mavenMetadataSearchPojo.version)
            .and(TMavenMetadataRecord::extension.name).`is`(mavenMetadataSearchPojo.extension)
        if (mavenMetadataSearchPojo.classifier == null) {
            criteria.and(TMavenMetadataRecord::classifier.name).exists(false)
        } else {
            criteria.and(TMavenMetadataRecord::classifier.name).`is`(mavenMetadataSearchPojo.classifier)
        }
        val query = Query(criteria)
        return mavenMetadataDao.find(query, TMavenMetadataRecord::class.java)
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")
        private val logger: Logger = LoggerFactory.getLogger(MavenMetadataService::class.java)
    }
}
