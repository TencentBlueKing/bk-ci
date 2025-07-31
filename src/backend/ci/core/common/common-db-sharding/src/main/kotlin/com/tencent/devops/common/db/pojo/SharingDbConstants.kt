/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.db.pojo

const val DATA_SOURCE_NAME_PREFIX = "ds_"
const val MIGRATING_DATA_SOURCE_NAME_PREFIX = "mig_ds_"
const val ARCHIVE_DATA_SOURCE_NAME_PREFIX = "archive_ds_"
const val DEFAULT_DATA_SOURCE_NAME = "ds_0"
const val DEFAULT_MIGRATING_DATA_SOURCE_NAME = "mig_ds_0"
const val DEFAULT_ARCHIVE_DATA_SOURCE_NAME = "archive_ds_0"
const val MIGRATING_SHARDING_DSL_CONTEXT = "migratingShardingDslContext" // 迁移库jooq上下文
const val ARCHIVE_SHARDING_DSL_CONTEXT = "archiveShardingDslContext" // 归档库jooq上下文
