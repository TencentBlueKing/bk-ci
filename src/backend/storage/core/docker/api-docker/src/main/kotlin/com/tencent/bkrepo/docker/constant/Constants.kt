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

package com.tencent.bkrepo.docker.constant

const val REPO_TYPE = "DOCKER"

const val BLOB_PATTERN = "/blobs"

const val MANIFEST_PATTERN = "/manifests"

const val USER_API_PREFIX = "/ext"
const val DOCKER_API_PREFIX = "/v2"
const val DOCKER_API_SUFFIX = "/auth"

// path pattern prefix
const val DOCKER_BLOB_SUFFIX = "{projectId}/{repoName}/**/blobs/uploads"
const val DOCKER_BLOB_UUID_SUFFIX = "{projectId}/{repoName}/**/blobs/uploads/{uuid}"
const val DOCKER_BLOB_DIGEST_SUFFIX = "{projectId}/{repoName}/**/blobs/{digest}"
const val DOCKER_MANIFEST_TAG_SUFFIX = "/{projectId}/{repoName}/**/manifests/{tag}"
const val DOCKER_MANIFEST_REFERENCE_SUFFIX = "/{projectId}/{repoName}/**/manifests/{reference}"
const val DOCKER_USER_MANIFEST_SUFFIX = "/manifest/{projectId}/{repoName}/**/{tag}"
const val DOCKER_USER_LAYER_SUFFIX = "/layer/{projectId}/{repoName}/**/{id}"
const val DOCKER_USER_REPO_SUFFIX = "/repo/{projectId}/{repoName}"
const val DOCKER_USER_TAG_SUFFIX = "/tag/{projectId}/{repoName}/**"
const val DOCKER_USER_DELETE_IMAGE_SUFFIX = "/package/delete/{projectId}/{repoName}"
const val DOCKER_USER_REPO_TAG_SUFFIX = "/version/delete/{projectId}/{repoName}"
const val DOCKER_USER_REPO_TAG_DETAIL_SUFFIX = "/version/detail/{projectId}/{repoName}"
const val DOCKER_REPO_ADDR = "/addr"
const val DOCKER_TAGS_SUFFIX = "/{projectId}/{repoName}/{name}/tags/list"

const val DOCKER_CATALOG_SUFFIX = "_catalog"
const val DOCKER_TMP_UPLOAD_PATH = "_uploads"

const val HTTP_FORWARDED_PROTO = "X-Forwarded-Proto"
const val HTTP_PROTOCOL_HTTP = "http"
const val HTTP_PROTOCOL_HTTPS = "https"
const val REGISTRY_SERVICE = "bkrepo"

const val ERROR_MESSAGE = "{\"errors\":[{\"code\":\"%s\",\"message\":\"%s\",\"detail\":\"%s\"}]}"
const val ERROR_MESSAGE_EMPTY = "{\"errors\":[{\"code\":\"%s\",\"message\":\"%s\",\"detail\":null}]}"
const val AUTH_CHALLENGE = "Bearer realm=\"%s\",service=\"%s\""
const val AUTH_CHALLENGE_SERVICE_SCOPE = "Bearer realm=\"%s\",service=\"%s\",scope=\"%s\""
const val AUTH_CHALLENGE_SCOPE = ",scope=\"%s:%s:%s\""
const val AUTH_CHALLENGE_TOKEN = "{\"token\": \"%s\", \"access_token\": \"%s\",\"issued_at\": \"%s\"}"
const val DOCKER_UNAUTHED_BODY =
    "{\"errors\":[{\"code\":\"UNAUTHORIZED\",\"message\":\"access to the " +
        "requested resource is not authorized\",\"detail\"" +
        ":[{\"Type\":\"repository\",\"Name\":\"samalba/my-app\",\"Action\":\"pull\"}," +
        "{\"Type\":\"repository\",\"Name\":\"samalba/my-app\",\"Action\":\"push\"}]}]}"

const val DOCKER_HEADER_API_VERSION = "Docker-Distribution-Api-Version"
const val DOCKER_API_VERSION = "registry/2.0"
const val DOCKER_CONTENT_DIGEST = "Docker-Content-Digest"
const val DOCKER_UPLOAD_UUID = "Docker-Upload-Uuid"

const val DOCKER_MANIFEST = "manifest.json"
const val DOCKER_MANIFEST_LIST = "list.manifest.json"
const val DOCKER_LENGTH_EMPTY = "0"

const val DOCKER_PROJECT_ID = "projectId"
const val DOCKER_REPO_NAME = "repoName"
const val DOCKER_NODE_PATH = "path"
const val DOCKER_NODE_NAME = "name"
const val DOCKER_NODE_SIZE = "size"
const val DOCKER_NODE_FULL_PATH = "fullPath"
const val DOCKER_DIGEST = "digest"
const val DOCKER_REFERENCE = "reference"
const val DOCKER_UUID = "uuid"
const val DOCKER_TAG = "tag"

const val DOCKER_MANIFEST_TYPE = "docker.manifest.type"

const val DOCKER_LINK = "Link"
const val DOCKER_EMPTY_CMD = " "
const val DOCKER_NOP_CMD = "(nop)"
const val DOCKER_NOP_SPACE_CMD = "(nop) "
const val DOCKER_RUN_CMD = "RUN"
const val DOCKER_HISTORY_CMD = "history"
const val DOCKER_LAYER_CMD = "layers"
const val DOCKER_CREATED_CMD = "created"
const val DOCKER_CREATED_BY_CMD = "created_by"
const val DOCKER_URLS_CMD = "urls"
const val DOCKER_COM_CMD = "v1Compatibility"
const val DOCKER_EMPTY_LAYER_CMD = "empty_layer"

const val DOCKER_MEDIA_TYPE = "mediaType"
const val DOCKER_FS_LAYER = "fsLayers"
const val DOCKER_BLOB_SUM = "blobSum"

const val DOCKER_SCHEMA_VERSION = "schemaVersion"
const val DOCKER_VERSION = "version"
const val DOCKER_VERSION_DOMAIN = "dockerDomain"
const val DOCKER_DIGEST_SHA256 = "sha256"
const val DOCKER_OS = "os"

const val DOCKER_FOREIGN_KEY = "application/vnd.docker.image.rootfs.foreign.diff.tar.gzip"

const val PAGE_NUMBER = "pageNumber"
const val PAGE_SIZE = "pageSize"

const val DOCKER_CREATE_BY = "createdBy"
const val DOCKER_CREATE_DATE = "createdDate"
const val LAST_MODIFIED_BY = "lastModifiedBy"
const val LAST_MODIFIED_DATE = "lastModifiedDate"
const val DOWNLOAD_COUNT = "downloadCount"
const val STAGE_TAG = "stageTag"
