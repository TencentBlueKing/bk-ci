package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import org.apache.maven.artifact.repository.metadata.Metadata
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MavenMetadataUtils {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private const val elementVersion = "0okm(IJN"

    fun Metadata.deleteVersioning(): Metadata {
        this.versioning.latest?.let {
            this.versioning.latest = this.versioning.versions.last()
        }
        this.versioning.release?.let {
            this.versioning.release = this.versioning.versions.apply {
                this.add(0, elementVersion)
            }.last { version ->
                !version.endsWith(SNAPSHOT_SUFFIX)
            }
            if (this.versioning.release == elementVersion) {
                this.versioning.release = null
            }
            this.versioning.versions.remove(elementVersion)
        }
        this.versioning.lastUpdated = LocalDateTime.now(ZoneId.of("UTC")).format(formatter)
        return this
    }
}
