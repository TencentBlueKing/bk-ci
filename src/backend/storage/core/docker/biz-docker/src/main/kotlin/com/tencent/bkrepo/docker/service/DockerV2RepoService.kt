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

package com.tencent.bkrepo.docker.service

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.pojo.DockerImage
import com.tencent.bkrepo.docker.pojo.DockerTag
import com.tencent.bkrepo.docker.pojo.DockerTagDetail
import com.tencent.bkrepo.docker.response.DockerResponse

/**
 * docker v2 protocol interface
 */
interface DockerV2RepoService {

    fun ping(): DockerResponse

    fun isBlobExists(context: RequestContext, digest: DockerDigest): DockerResponse

    fun getBlob(context: RequestContext, digest: DockerDigest): DockerResponse

    fun startBlobUpload(context: RequestContext, mount: String?): DockerResponse

    fun patchUpload(context: RequestContext, uuid: String, file: ArtifactFile): DockerResponse

    fun uploadBlob(context: RequestContext, digest: DockerDigest, uuid: String, file: ArtifactFile): DockerResponse

    fun uploadManifest(context: RequestContext, tag: String, mediaType: String, file: ArtifactFile): DockerResponse

    fun getManifest(context: RequestContext, reference: String): DockerResponse

    fun deleteManifest(context: RequestContext, reference: String): DockerResponse

    fun getTags(context: RequestContext, maxEntries: Int, lastEntry: String): DockerResponse

    fun catalog(context: RequestContext, maxEntries: Int, lastEntry: String): DockerResponse

    fun getRepoList(context: RequestContext, pageNumber: Int, pageSize: Int, name: String?): List<DockerImage>

    fun getRepoTagList(
        context: RequestContext,
        pageNumber: Int,
        pageSize: Int,
        tag: String?
    ): List<DockerTag>

    fun buildLayerResponse(context: RequestContext, layerId: String): DockerResponse

    fun getManifestString(context: RequestContext, tag: String): String

    fun deleteTag(context: RequestContext, tag: String): Boolean

    fun getRepoTagDetail(context: RequestContext, tag: String): DockerTagDetail?
}
