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

package com.tencent.bkrepo.rpm.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.rpm.exception.RpmRequestParamMissException
import com.tencent.bkrepo.rpm.pojo.RpmPackagePojo
import com.tencent.bkrepo.rpm.pojo.RpmVersion

object RpmVersionUtils {

    /**
     * filename解析rpm构件版本信息
     */
    @Deprecated("作为备选项保留")
    fun resolverRpmVersion(filename: String): RpmVersion {
        try {
            val strList = filename.split("-")
            val suffixFormat = strList.last()
            val suffixList = suffixFormat.split(".")
            val rel = suffixList[0]
            val arch = suffixFormat.removePrefix("$rel.").removeSuffix(".rpm")
            val ver = strList[strList.size - 2]
            val name = filename.removeSuffix("-$ver-$suffixFormat")
            return RpmVersion(name, arch, "0", ver, rel)
        } catch (indexOutOfBoundsException: IndexOutOfBoundsException) {
            throw RpmRequestParamMissException("$filename format error")
        }
    }

    @Deprecated("作为备选项保留")
    fun String.toRpmPackagePojo(): RpmPackagePojo {
        val path = this.substringBeforeLast("/").removePrefix("/")
        val rpmArtifactName = this.substringAfterLast("/")
        val rpmVersion = resolverRpmVersion(rpmArtifactName)
        return RpmPackagePojo(
            path = path,
            name = rpmVersion.name,
            version = "${rpmVersion.ver}-${rpmVersion.rel}.${rpmVersion.arch}"
        )
    }

    fun RpmVersion.toMetadata(): MutableMap<String, String> {
        return mutableMapOf(
            "name" to this.name,
            "arch" to this.arch,
            "epoch" to this.epoch,
            "ver" to this.ver,
            "rel" to this.rel
        )
    }

    fun Map<String, Any>.toRpmVersion(artifactUri: String): RpmVersion {
        return RpmVersion(
            this["name"] as String? ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                "$artifactUri: not found metadata.name value"
            ),
            this["arch"] as String? ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                "$artifactUri: not found metadata.arch value"
            ),
            this["epoch"] as String? ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                "$artifactUri: not found metadata.epoch value"
            ),
            this["ver"] as String? ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                "$artifactUri: not found metadata.ver value"
            ),
            this["rel"] as String? ?: throw ErrorCodeException(
                CommonMessageCode.RESOURCE_NOT_FOUND,
                "$artifactUri: not found metadata.rel value"
            )
        )
    }
}
