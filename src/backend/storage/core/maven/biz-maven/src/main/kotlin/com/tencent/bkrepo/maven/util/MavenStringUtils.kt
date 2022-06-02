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

package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.constants.ARTIFACT_FORMAT
import com.tencent.bkrepo.maven.constants.MAVEN_METADATA_FILE_NAME
import com.tencent.bkrepo.maven.constants.PACKAGE_SUFFIX_REGEX
import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import com.tencent.bkrepo.maven.constants.TIMESTAMP_FORMAT
import com.tencent.bkrepo.maven.enum.SnapshotBehaviorType
import com.tencent.bkrepo.maven.exception.MavenArtifactFormatException
import com.tencent.bkrepo.maven.pojo.MavenRepoConf
import com.tencent.bkrepo.maven.pojo.MavenVersion
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import java.util.regex.Pattern

object MavenStringUtils {

    private const val JAVA_ARCHIVE = "application/java-archive"
    private const val X_MAVEN_POM = "application/x-maven-pom+xml"
    private const val MAVEN_XML = "application/xml"

    fun String.formatSeparator(oldSeparator: String, newSeparator: String): String {
        val strList = this.removePrefix(oldSeparator).removeSuffix(oldSeparator).split(oldSeparator)
        return StringUtils.join(strList, newSeparator)
    }

    fun String.fileMimeType(): String? {
        return if (this.endsWith("jar")) {
            JAVA_ARCHIVE
        } else if (this.endsWith("pom")) {
            X_MAVEN_POM
        } else if (this.endsWith("xml")) {
            MAVEN_XML
        } else null
    }

    fun String.httpStatusCode(repoConf: MavenRepoConf): Int {
        return if (this.endsWith(MAVEN_METADATA_FILE_NAME) && this.isSnapshotUri() &&
            repoConf.mavenSnapshotVersionBehavior != SnapshotBehaviorType.DEPLOYER
        ) {
            HttpStatus.SC_ACCEPTED
        } else if (this.endsWith("maven-metadata.xml.md5") || this.endsWith("maven-metadata.xml.sha1")) {
            HttpStatus.SC_OK
        } else HttpStatus.SC_CREATED
    }

    /**
     * e.g. *1.0-SNAPSHOT/1.0-*.jar   [Boolean] = true
     */
    fun String.isSnapshotUri(): Boolean {
        return this.substringBeforeLast('/').endsWith(SNAPSHOT_SUFFIX)
    }

    /**
     * e.g. *1.0-SNAPSHOT/1.0-SNAPSHOT.jar   [Boolean] = true
     * e.g. *1.0-SNAPSHOT/1.0-20211228.172345.jar   [Boolean] = false
     */
    fun String.isSnapshotNonUniqueUri(): Boolean {
        return this.isSnapshotUri() &&
            this.substringAfterLast("/").contains(SNAPSHOT_SUFFIX)
    }

    /**
     * e.g. *1.0-SNAPSHOT/1.0-SNAPSHOT.jar   [Boolean] = false
     * e.g. *1.0-SNAPSHOT/xxx   [Boolean] = true
     */
    fun String.isSnapshotUniqueUri(): Boolean {
        if (this.isSnapshotUri()) {
            val suffix = this.substringAfterLast("/")
            return !suffix.contains(SNAPSHOT_SUFFIX) && !suffix.startsWith(MAVEN_METADATA_FILE_NAME)
        }
        return false
    }

    /**
     * 将maven 包名转为[MavenVersion]
     * 完整请求路径 e.g. /com/mycompany/app/my-app/1.0-SNAPSHOT/my-app-1.0-20211129.073728-8.jar
     * [this] 请求路径中完整包名 e.g. my-app-1.0-20211129.073728-8.jar
     * [artifactId] = my-app
     * [version] = 1.0-SNAPSHOT
     * @return [MavenVersion.timestamp] = 20211129.073728
     * @return [MavenVersion.buildNo] = 8
     * @return [MavenVersion.classifier] = null
     * @return [MavenVersion.packaging] = jar
     */
    fun String.resolverName(artifactId: String, version: String): MavenVersion {
        val matcher = Pattern.compile(PACKAGE_SUFFIX_REGEX).matcher(this)
        if (matcher.matches()) {
            val packaging = matcher.group(2)
            val mavenVersion = MavenVersion(
                artifactId = artifactId,
                version = version,
                packaging = packaging
            )
            mavenVersion.setVersion(this)
            return mavenVersion
        }
        throw MavenArtifactFormatException(this)
    }

    fun MavenVersion.setVersion(artifactName: String) {
        val artifactNameRegex = String.format(
            ARTIFACT_FORMAT,
            this.artifactId,
            this.version.removeSuffix(SNAPSHOT_SUFFIX),
            this.packaging
        )
        val matcher = Pattern.compile(artifactNameRegex).matcher(artifactName)
        if (matcher.matches()) {
            val timestampStr = matcher.group(1)
            if (timestampStr != null && timestampStr != SNAPSHOT_SUFFIX.trim('-')) {
                val timeMatch = Pattern.compile(TIMESTAMP_FORMAT).matcher(timestampStr)
                if (timeMatch.matches()) {
                    this.timestamp = timeMatch.group(1)
                    this.buildNo = timeMatch.group(2).toInt()
                }
            }
            this.classifier = matcher.group(2)
        }
    }
}
