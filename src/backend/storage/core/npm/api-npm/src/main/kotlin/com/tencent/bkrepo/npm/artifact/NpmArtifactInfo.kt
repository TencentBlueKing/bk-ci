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

package com.tencent.bkrepo.npm.artifact

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo

class NpmArtifactInfo(
    projectId: String,
    repoName: String,
    artifactUri: String
) : ArtifactInfo(projectId, repoName, artifactUri) {

    companion object {
        // publish package
        const val NPM_PKG_PUBLISH_MAPPING_URI = "/{projectId}/{repoName}/*"
        const val NPM_SCOPE_PKG_PUBLISH_MAPPING_URI = "/{projectId}/{repoName}/*/*"

        const val NPM_UNPUBLISH_MAPPING_URI = "/{projectId}/{repoName}/*/-rev/{rev}"
        const val NPM_UNPUBLISH_SCOPE_MAPPING_URI = "/{projectId}/{repoName}/*/*/-rev/{rev}"

        const val NPM_UNPUBLISH_VERSION_MAPPING_URI =
            "/**/{projectId}/{repoName}/{name}/{delimiter:-|download}/{filename}/-rev/{rev}"
        const val NPM_UNPUBLISH_VERSION_SCOPE_MAPPING_URI =
            "/**/{projectId}/{repoName}/{scope}/{name}/{delimiter:-|download}/{scope}/{filename}/-rev/{rev}"

        // package uri model
        const val NPM_SCOPE_PACKAGE_VERSION_INFO_MAPPING_URI =
            "/{projectId}/{repoName}/{scope}/{pkgName}/{version}"
        const val NPM_PACKAGE_INFO_MAPPING_URI = "/{projectId}/{repoName}/{pkgName}"
        const val NPM_PACKAGE_VERSION_INFO_MAPPING_URI = "/{projectId}/{repoName}/{pkgName}/{version}"

        // npm https://registry.npmjs.org/
        const val NPM_PACKAGE_TGZ_MAPPING_URI = "/{projectId}/{repoName}/**/*.tgz"

        // search
        const val NPM_PACKAGE_SEARCH_MAPPING_URI = "/{projectId}/{repoName}/-/v1/search"

        // dist-tag
        const val NPM_PACKAGE_DIST_TAG_INFO_MAPPING_URI = "/{projectId}/{repoName}/-/package/**/dist-tags"
        const val NPM_PACKAGE_DIST_TAG_ADD_MAPPING_URI = "/{projectId}/{repoName}/-/package/**/dist-tags/*"

        // auth user
        const val NPM_ADD_USER_MAPPING_URI = "/{projectId}/{repoName}/-/user/org.couchdb.user:*"
        const val NPM_USER_LOGOUT_MAPPING_URI = "/{projectId}/{repoName}/-/user/token/*"
        const val NPM_WHOAMI_MAPPING_URI = "/{projectId}/{repoName}/-/whoami"
    }
}
