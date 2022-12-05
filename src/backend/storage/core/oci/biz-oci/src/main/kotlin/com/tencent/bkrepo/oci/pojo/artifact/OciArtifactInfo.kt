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

package com.tencent.bkrepo.oci.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo

/**
 * oci 构件基本信息
 * 其余场景的ArtifactInfo 可以继承该类，如[OciBlobArtifactInfo]
 */
open class OciArtifactInfo(
    projectId: String,
    repoName: String,
    val packageName: String,
    val version: String
) : ArtifactInfo(projectId, repoName, StringPool.EMPTY) {
    override fun getArtifactName() = packageName

    override fun getArtifactVersion() = version

    companion object {
        // manifest check/pull/upload/delete
        const val MANIFEST_URL = "/v2/{projectId}/{repoName}/**/manifests/{reference}"

        // blobs check/pull/delete
        const val BOLBS_URL = "/v2/{projectId}/{repoName}/**/blobs/{digest}"

        // blobs upload
        const val BOLBS_UPLOAD_FIRST_STEP_URL = "/v2/{projectId}/{repoName}/**/blobs/uploads/"
        const val BOLBS_UPLOAD_SECOND_STEP_URL = "/v2/{projectId}/{repoName}/**/blobs/uploads/{uuid}"

        // tags get
        const val TAGS_URL = "/v2/{projectId}/{repoName}/**/tags/list"

        // version详情获取
        const val OCI_VERSION_DETAIL = "/version/detail/{projectId}/{repoName}"

        // 额外的package或者version 删除接口
        const val OCI_PACKAGE_DELETE_URL = "/package/delete/{projectId}/{repoName}"
        const val OCI_VERSION_DELETE_URL = "/version/delete/{projectId}/{repoName}"
        const val OCI_USER_MANIFEST_SUFFIX = "/manifest/{projectId}/{repoName}/**/{tag}"
        const val OCI_USER_LAYER_SUFFIX = "/layer/{projectId}/{repoName}/**/{id}"
        const val OCI_USER_REPO_SUFFIX = "/repo/{projectId}/{repoName}"
        const val OCI_USER_TAG_SUFFIX = "/tag/{projectId}/{repoName}/**"
        const val DOCKER_CATALOG_SUFFIX = "_catalog"
    }
}
