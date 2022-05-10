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

package com.tencent.bkrepo.common.storage.util

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

fun String.toPath(): Path = Paths.get(this)

fun Path.createFile(): File {
    if (!Files.isRegularFile(this)) {
        if (this.parent != null) {
            Files.createDirectories(this.parent)
        }
        try {
            Files.createFile(this)
        } catch (ignored: java.nio.file.FileAlreadyExistsException) {
            // ignore
        }
    }
    return this.toFile()
}

fun Path.createNewOutputStream(): OutputStream {
    if (!Files.isDirectory(this.parent)) {
        Files.createDirectories(this.parent)
    }
    return Files.newOutputStream(
        this,
        StandardOpenOption.CREATE_NEW
    )
}

/**
 * 删除路径，如果路径为文件或者空目录则删除
 * @return true表示已经执行了删除，false表示未执行删除
 * */
fun Path.delete(): Boolean {
    // 文件
    if (this.toFile().isFile) {
        Files.deleteIfExists(this)
        return true
    }
    // 删除空目录
    Files.newDirectoryStream(this).use {
        if (!it.iterator().hasNext()) {
            Files.deleteIfExists(this)
            return true
        }
    }
    // 目录还存在内容
    return false
}
