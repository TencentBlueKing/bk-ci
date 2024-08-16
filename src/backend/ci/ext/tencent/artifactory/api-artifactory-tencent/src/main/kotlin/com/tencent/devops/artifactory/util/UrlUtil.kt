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

package com.tencent.devops.artifactory.util

import com.tencent.devops.common.service.utils.HomeHostUtil

object UrlUtil {
    fun toOuterPhotoAddr(innerPhotoAddr: String?): String {

        if (innerPhotoAddr == null) return ""

        if (innerPhotoAddr.contains("bkdevops.qq.com")) {
            return innerPhotoAddr
        }

        return if (innerPhotoAddr.contains("bkrepo") && innerPhotoAddr.contains("generic")) { // 仓库存储
            "${HomeHostUtil.outerServerHost()}/bkrepo/api/external/generic" + innerPhotoAddr.split("generic")[1]
        } else if (innerPhotoAddr.contains("radosgw.open")) { // s3存储
            innerPhotoAddr.replace(
                Regex("http(s|)://radosgw.open.(w|)oa.com"),
                "${HomeHostUtil.outerServerHost()}/images"
            )
        } else if (innerPhotoAddr.contains("staticfile.woa.com")) {
            innerPhotoAddr.replace(
                Regex("https://(dev|test|)staticfile.woa.com"),
                "${HomeHostUtil.outerServerHost()}/bkrepo/api/staticfile"
            ).let { if (it.contains("?v=")) it.split("?v=")[0] else it }
        } else {
            innerPhotoAddr
        }
    }
}
