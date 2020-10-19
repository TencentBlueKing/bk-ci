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

import com.tencent.bkrepo.docker.artifact.DockerArtifactRepo
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.ManifestMetadata

/**
 * the enterypoint for deserialize manifest
 */
object ManifestDeserializer {

    /**
     * deserialize the manifest file of the docker registry
     * @param repo docker repo to work with the storage
     * @param context the request context params
     * @param tag the docker image tag
     * @param manifestType the type of the manifest
     * @param bytes ByteArray of the manifest file
     * @param digest the digest object
     * @return ManifestMetadata
     */
    fun deserialize(repo: DockerArtifactRepo, context: RequestContext, tag: String, manifestType: ManifestType, bytes: ByteArray, digest: DockerDigest): ManifestMetadata {
        var manifestBytes = bytes
        val manifestProcess = ManifestProcess(repo)
        return when (manifestType) {
            ManifestType.Schema1 -> ManifestSchema1Deserializer.deserialize(manifestBytes, digest)
            ManifestType.Schema1Signed -> ManifestSchema1Deserializer.deserialize(manifestBytes, digest)
            ManifestType.Schema2 -> {
                val configBytes = manifestProcess.getSchema2ConfigContent(context, manifestBytes, tag)
                ManifestSchema2Deserializer.deserialize(manifestBytes, configBytes, context.artifactName, tag, digest)
            }
            ManifestType.Schema2List -> {
                val schema2Path = manifestProcess.getSchema2Path(context, manifestBytes)
                manifestBytes = manifestProcess.getSchema2ManifestContent(context, schema2Path)
                ManifestSchema1Deserializer.deserialize(manifestBytes, digest)
            }
        }
    }
}
