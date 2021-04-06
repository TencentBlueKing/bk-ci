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

package com.tencent.bkrepo.npm.constants

const val REPO_TYPE = "NPM"

const val APPLICATION_OCTET_STEAM = "mime_type"

/**
 * 文件分隔符
 */
const val FILE_SEPARATOR = "/"
const val FILE_DASH = "-"
const val FILE_SUFFIX = ".tgz"

const val TGZ_FULL_PATH_WITH_DASH_SEPARATOR = "/-/"

const val ATTRIBUTE_OCTET_STREAM_SHA1 = "artifact.sha1.octet-stream"

// fileName
const val NPM_PACKAGE_TGZ_FILE = "npm_package_tgz_file"
const val NPM_PACKAGE_VERSION_JSON_FILE = "npm_package_version_json_file"
const val NPM_PACKAGE_JSON_FILE = "npm_package_json_file"

// full path
const val NPM_PKG_TGZ_FILE_FULL_PATH = NPM_PACKAGE_TGZ_FILE + "_full_path"
const val NPM_PKG_VERSION_JSON_FILE_FULL_PATH = NPM_PACKAGE_VERSION_JSON_FILE + "_full_path"
const val NPM_PKG_JSON_FILE_FULL_PATH = NPM_PACKAGE_JSON_FILE + "_full_path"
// full path value
const val NPM_PKG_TGZ_FULL_PATH = "/%s/-/%s-%s.tgz"
const val NPM_PKG_TGZ_WITH_DOWNLOAD_FULL_PATH = "/%s/download/%s-%s.tgz"
const val NPM_PKG_VERSION_METADATA_FULL_PATH = "/.npm/%s/%s-%s.json"
const val NPM_PKG_METADATA_FULL_PATH = "/.npm/%s/package.json"

const val NPM_FILE_FULL_PATH = "npm_file_full_path"

const val SEARCH_REQUEST = "search_request"

const val PKG_NAME = "pkg_name"

// constants map
val ERROR_MAP = mapOf("error" to "not_found", "reason" to "document not found")

const val NPM_TGZ_TARBALL_PREFIX = "X-BKREPO-NPM-PREFIX"
