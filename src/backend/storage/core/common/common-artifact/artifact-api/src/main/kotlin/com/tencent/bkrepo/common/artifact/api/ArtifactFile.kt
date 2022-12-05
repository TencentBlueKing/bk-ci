/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.artifact.api

import com.tencent.bkrepo.common.artifact.file.FileHashAccessor
import java.io.File
import java.io.InputStream

/**
 * 构件文件接口
 */
interface ArtifactFile : FileHashAccessor {

    /**
     * 获取文件流，使用完需要手动关闭
     */
    fun getInputStream(): InputStream

    /**
     * 返回文件大小，单位字节
     */
    fun getSize(): Long

    /**
     * 判断文件是否在内存中
     */
    fun isInMemory(): Boolean

    /**
     * 获取文件对象。当文件在内存中时，返回空
     */
    fun getFile(): File?

    /**
     * 强制将文件数据写入到文件中，并返回该文件对象
     */
    fun flushToFile(): File

    /**
     * 删除文件
     */
    fun delete()

    /**
     * 判断是否初始化
     */
    fun hasInitialized(): Boolean

    /**
     * 判断数据存储过程是否降级到本地磁盘
     */
    fun isFallback(): Boolean

    /**
     * 判断文件是否在本地磁盘
     * */
    fun isInLocalDisk(): Boolean
}
