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

package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.repository.model.TPackage.Companion.PACKAGE_KEY_IDX
import com.tencent.bkrepo.repository.model.TPackage.Companion.PACKAGE_KEY_IDX_DEF
import com.tencent.bkrepo.repository.model.TPackage.Companion.PACKAGE_NAME_IDX
import com.tencent.bkrepo.repository.model.TPackage.Companion.PACKAGE_NAME_IDX_DEF
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 包模型
 */
@Document("package")
@CompoundIndexes(
    CompoundIndex(name = PACKAGE_NAME_IDX, def = PACKAGE_NAME_IDX_DEF, background = true),
    CompoundIndex(name = PACKAGE_KEY_IDX, def = PACKAGE_KEY_IDX_DEF, background = true, unique = true)
)
data class TPackage(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    var projectId: String,
    var repoName: String,
    var name: String,
    var key: String,
    var type: PackageType,
    var latest: String? = null,
    var downloads: Long,
    var versions: Long,
    var description: String? = null,
    var versionTag: Map<String, String>? = null,
    var extension: Map<String, Any>? = null
) {
    companion object {
        const val PACKAGE_NAME_IDX = "package_name_idx"
        const val PACKAGE_KEY_IDX = "package_key_idx"
        const val PACKAGE_NAME_IDX_DEF = "{'projectId': 1, 'repoName': 1, 'name': 1}"
        const val PACKAGE_KEY_IDX_DEF = "{'projectId': 1, 'repoName': 1, 'key': 1}"
    }
}
