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

package com.tencent.bkrepo.rds.utils

import com.tencent.bkrepo.rds.constants.INDEX_CACHE_YAML
import com.tencent.bkrepo.rds.constants.INDEX_YAML
import com.tencent.bkrepo.rds.constants.TGZ_SUFFIX
import com.tencent.bkrepo.rds.constants.V1
import com.tencent.bkrepo.rds.pojo.metadata.RdsIndexYamlMetadata

object RdsUtils {

    fun getChartFileFullPath(name: String, version: String, extension: String = TGZ_SUFFIX): String {
        return "/%s-%s.%s".format(name, version, extension)
    }

    fun getIndexCacheYamlFullPath(): String {
        return "/$INDEX_CACHE_YAML"
    }

    fun getIndexYamlFullPath(): String {
        return "/$INDEX_YAML"
    }

    fun initIndexYamlMetadata(): RdsIndexYamlMetadata {
        return RdsIndexYamlMetadata(
            apiVersion = V1,
            generated = TimeFormatUtil.getUtcTime()
        )
    }

    /**
     * remote仓库下载index.yaml时使用的是index.yaml, 需要做个转换
     */
    fun convertIndexYamlPath(path: String): String {
        return when (path) {
            "/" -> getIndexYamlFullPath()
            getIndexCacheYamlFullPath() -> getIndexYamlFullPath()
            else -> path
        }
    }

    /**
     * index.yaml存储时使用的是index-cache.yaml, 需要做个转换
     */
    fun convertIndexYamlPathToCache(path: String): String {
        return when (path) {
            "/" -> getIndexCacheYamlFullPath()
            else -> {
                path.substring(path.lastIndexOf('/'))
            }
        }
    }
}
