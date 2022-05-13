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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.maven.constants

const val CHECKSUM_POLICY = "CHECKSUM_POLICY"
const val SNAPSHOT_BEHAVIOR = "SNAPSHOT_BEHAVIOR"
const val MAX_UNIQUE_SNAPSHOTS = "MAX_UNIQUE_SNAPSHOTS"
const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
const val MAVEN_METADATA_FILE_NAME = "maven-metadata.xml"

const val X_CHECKSUM_SHA1 = "X-Checksum-Sha1"
const val FULL_PATH = "fullPath"
const val PACKAGE_KEY = "packageKey"
const val VERSION = "version"


const val PACKAGE_SUFFIX_REGEX =
    "(.+)\\.(jar|war|tar|ear|ejb|rar|msi|aar|module|kar|rpm|tar\\.bz2|tar\\.gz|tar\\.xz|tbz|zip|pom)\$"

const val ARTIFACT_FORMAT = "^%s-%s-?(SNAPSHOT|[0-9]{8}\\.[0-9]{6}-[0-9]+)?-?(.+)?.%s\$"

const val TIMESTAMP_FORMAT = "([0-9]{8}\\.[0-9]{6})-([0-9]+)"
