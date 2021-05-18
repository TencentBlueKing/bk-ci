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

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import java.io.File
import java.io.InputStream

/**
 * 文件存储接口
 */
interface FileStorage {
    /**
     * 保存文件
     * @param path 文件保存路径
     * @param name 文件保存名称
     * @param file 文件
     * @param storageCredentials 存储凭证
     */
    fun store(path: String, name: String, file: File, storageCredentials: StorageCredentials)

    /**
     * 保存数据流
     * @param path 文件保存路径
     * @param name 文件保存名称
     * @param inputStream 数据输入流
     * @param size 流长度，通过inputStream取得的长度不准确，可以出现int溢出
     * @param storageCredentials 存储凭证
     */
    fun store(path: String, name: String, inputStream: InputStream, size: Long, storageCredentials: StorageCredentials)

    /**
     * 加载数据流
     * @param path 文件所在路径
     * @param name 文件名称
     * @param range 数据加载范围
     * @param storageCredentials 存储凭证
     *
     * @return 数据流
     */
    fun load(path: String, name: String, range: Range, storageCredentials: StorageCredentials): InputStream?

    /**
     * 删除文件
     * @param path 文件所在路径
     * @param name 文件名称
     * @param storageCredentials 存储凭证
     */
    fun delete(path: String, name: String, storageCredentials: StorageCredentials)

    /**
     * 判断文件是否存在
     * @param path 文件所在路径
     * @param name 文件名称
     * @param storageCredentials 存储凭证
     */
    fun exist(path: String, name: String, storageCredentials: StorageCredentials): Boolean

    /**
     * 在不同存储实例之间拷贝文件
     * @param path 文件所在路径
     * @param name 文件名称
     * @param fromCredentials 源存储凭证
     * @param toCredentials 目的存储凭证
     */
    fun copy(path: String, name: String, fromCredentials: StorageCredentials, toCredentials: StorageCredentials)

    /**
     * 获取存储的临时目录，默认实现返回`java.io.tmpdir`目录
     * @param storageCredentials 存储凭证
     */
    fun getTempPath(storageCredentials: StorageCredentials): String = System.getProperty("java.io.tmpdir")
}
