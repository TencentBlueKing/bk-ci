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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.storage.core.StorageProperties
import org.springframework.util.unit.DataSize
import javax.servlet.MultipartConfigElement

class UploadConfigElement(
    private val storageProperties: StorageProperties
) : MultipartConfigElement(storageProperties.defaultStorageCredentials().upload.location) {

    init {
        if (storageProperties.receive.maxFileSize.isNegative) {
            storageProperties.receive.maxFileSize = DataSize.ofBytes(-1)
        }
        if (storageProperties.receive.maxRequestSize.isNegative) {
            storageProperties.receive.maxRequestSize = DataSize.ofBytes(-1)
        }
        if (storageProperties.receive.fileSizeThreshold.isNegative) {
            storageProperties.receive.fileSizeThreshold = DataSize.ofBytes(-1)
        }
    }

    override fun getLocation(): String {
        return storageProperties.defaultStorageCredentials().upload.location
    }

    override fun getMaxFileSize(): Long {
        return storageProperties.receive.maxFileSize.toBytes()
    }

    override fun getMaxRequestSize(): Long {
        return storageProperties.receive.maxRequestSize.toBytes()
    }

    override fun getFileSizeThreshold(): Int {
        return storageProperties.receive.fileSizeThreshold.toBytes().toInt()
    }
}
