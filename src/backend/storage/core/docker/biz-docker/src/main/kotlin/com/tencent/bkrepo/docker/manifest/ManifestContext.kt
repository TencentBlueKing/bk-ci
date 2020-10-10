/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.manifest

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_TYPE
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.context.UploadContext
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.ManifestMetadata

/**
 * manifest context to describe
 * interactive params
 */
object ManifestContext {

    private const val DOCKER_MANIFEST_DIGEST = "docker.manifest.digest"
    private const val DOCKER_MANIFEST_NAME = "docker.manifest"
    private const val DOCKER_NAME_REPO = "docker.repoName"

    /**
     * build the docker image property
     * @param dockerRepo docker image name
     * @param tag docker tag
     * @param digest docker image digest
     * @param type docker image manifest type
     * @return HashMap metadata property
     */
    fun buildPropertyMap(
        dockerRepo: String,
        tag: String,
        digest: DockerDigest,
        type: ManifestType
    ): HashMap<String, String> {
        var map = HashMap<String, String>()
        map.apply {
            set(digest.getDigestAlg(), digest.getDigestHex())
        }.apply {
            set(DOCKER_MANIFEST_DIGEST, digest.toString())
        }.apply {
            set(DOCKER_MANIFEST_NAME, tag)
        }.apply {
            set(DOCKER_NAME_REPO, dockerRepo)
        }.apply {
            set(DOCKER_MANIFEST_TYPE, type.toString())
        }
        return map
    }

    /**
     * build manifest list upload context
     * @param context the request context
     * @param digest the digest of docker image
     * @param path docker path
     * @param bytes byte data of manifest
     */
    fun buildManifestListUploadContext(
        context: RequestContext,
        digest: DockerDigest,
        path: String,
        bytes: ByteArray
    ): UploadContext {
        with(context) {
            val artifactFile = ArtifactFileFactory.build(bytes.inputStream())
            return UploadContext(projectId, repoName, path).artifactFile(artifactFile).sha256(digest.getDigestHex())
        }
    }

    /**
     * build the manifest upload context
     * @param context the request context
     * @param type the manifest file type
     * @param metadata the metadata of manifest
     * @param path the manifest path
     * @param file upload  file object
     */
    fun buildUploadContext(
        context: RequestContext,
        type: ManifestType,
        metadata: ManifestMetadata,
        path: String,
        file: ArtifactFile
    ): UploadContext {
        with(context) {
            val uploadContext = UploadContext(projectId, repoName, path).artifactFile(file)
            if ((type == ManifestType.Schema2 || type == ManifestType.Schema2List)) {
                uploadContext.sha256(metadata.tagInfo.digest!!.getDigestHex())
            }
            return uploadContext
        }
    }
}
