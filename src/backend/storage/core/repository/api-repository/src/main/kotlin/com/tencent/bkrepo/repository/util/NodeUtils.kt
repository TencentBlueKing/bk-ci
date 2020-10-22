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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode.NODE_PATH_INVALID

/**
 * 节点相关工具类
 * path节点目录命名规则：以'/'开头，以'/'结尾
 * name节点文件命名规则：不含'/'
 * fullpath全路径命名规则：以'/'开头，结尾不含'/'
 */
object NodeUtils {

    private val forbiddenNameList = listOf(".", "..")

    private val keywordList = listOf("\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|", "?", "&")
    /**
     * 最大目录深度
     */
    private const val MAX_DIR_DEPTH = 64

    /**
     * 文件名称最大长度
     */
    private const val MAX_FILENAME_LENGTH = 1024

    /**
     * 文件分隔符
     */
    const val FILE_SEPARATOR = "/"

    /**
     * 文件分隔符
     */
    private const val DOT = "."

    /**
     * 文件分隔字符
     */
    private const val FILE_SEPARATOR_CHAR = '/'

    /**
     * 根目录
     */
    const val ROOT_PATH = FILE_SEPARATOR

    /**
     * 格式化目录名称, 返回格式/a/b/c/，根目录返回/
     * /a/b/c -> /a/b/c/
     * /a/b/c/ -> /a/b/c/
     */
    fun formatPath(input: String): String {
        val path = formatFullPath(input)
        return if (isRootPath(path)) ROOT_PATH else path + FILE_SEPARATOR
    }

    /**x
     * 格式化全路径名称, 返回格式/a/b/c，根目录返回/
     */
    fun formatFullPath(input: String): String {
        val path = input.trim()
        if (isRootPath(path)) return ROOT_PATH

        val nameList = path.split(FILE_SEPARATOR)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != StringPool.DOT }
            .toList()
        val builder = StringBuilder()
        nameList.takeIf { it.isEmpty() }?.apply { builder.append(FILE_SEPARATOR) }
        nameList.forEach { builder.append(FILE_SEPARATOR).append(it) }
        return builder.toString()
    }

    /**
     * 解析目录名称，返回格式/a/b/c/，根目录返回/
     * 出错则抛出异常
     */
    fun parseFullPath(input: String): String {
        val fullPath = input.trim()
        if (isRootPath(fullPath)) return ROOT_PATH

        fullPath.takeIf { it.startsWith(FILE_SEPARATOR) } ?: throw ErrorCodeException(NODE_PATH_INVALID, input)

        val nameList = fullPath.split(FILE_SEPARATOR)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != StringPool.DOT }
            .map { validateFileName(it) }
            .toList()
        nameList.takeIf { it.size <= MAX_DIR_DEPTH } ?: throw ErrorCodeException(NODE_PATH_INVALID, input)

        val builder = StringBuilder()
        nameList.takeIf { it.isEmpty() }?.apply { builder.append(FILE_SEPARATOR) }
        nameList.forEach { builder.append(FILE_SEPARATOR).append(it) }
        return builder.toString()
    }

    /**
     * 处理并验证文件名称，返回格式abc.txt
     * 不能包含/，不能全为空，不能超过指定长度
     */
    fun validateFileName(input: String): String {
        try {
            require(input.isNotBlank())
            require(input.length <= MAX_FILENAME_LENGTH)
            require(!forbiddenNameList.contains(input))
            require(!input.contains(FILE_SEPARATOR))
        } catch (exception: IllegalArgumentException) {
            throw ErrorCodeException(NODE_PATH_INVALID, input)
        }
        return input
    }

    /**
     * 根据路径列表和文件名组合全路径，返回格式/a/b/c/abc.txt
     */
    fun combineFullPath(path: String, name: String): String {
        return if (!path.endsWith(FILE_SEPARATOR)) path + FILE_SEPARATOR + name else path + name
    }

    /**
     * 根据路径列表和文件名组合新的路径，返回格式/a/b/c/
     */
    fun combinePath(parent: String, name: String): String {
        val parentPath = if (!parent.endsWith(FILE_SEPARATOR)) parent + FILE_SEPARATOR else parent
        val newPath = parentPath + name
        return if (!newPath.endsWith(FILE_SEPARATOR)) newPath + FILE_SEPARATOR else newPath
    }

    /**
     * 获取父级目录，返回格式/a/b/c/
     */
    fun getParentPath(path: String): String {
        val index = path.trimEnd(FILE_SEPARATOR_CHAR).lastIndexOf(FILE_SEPARATOR)
        return if (isRootPath(path) || index <= 0) ROOT_PATH else path.substring(0, index + 1)
    }

    /**
     * 根据目录获取文件名称，返回格式abc.txt
     */
    fun getName(path: String): String {
        val trimmedPath = path.trimEnd(FILE_SEPARATOR_CHAR)
        return if (isRootPath(trimmedPath)) "" else trimmedPath.substring(trimmedPath.lastIndexOf(FILE_SEPARATOR) + 1)
    }

    /**
     * 判断路径是否为根目录
     */
    fun isRootPath(path: String): Boolean {
        return path == ROOT_PATH || path == ""
    }

    /**
     * 正则特殊符号转义
     */
    fun escapeRegex(input: String): String {
        var escapedString = input.trim()
        if (escapedString.isNotBlank()) {
            keywordList.forEach {
                if (escapedString.contains(it)) {
                    escapedString = escapedString.replace(it, "\\$it")
                }
            }
        }
        return escapedString
    }

    /**
     * 获取文件后缀
     */
    fun getExtension(fileName: String): String? {
        return fileName.trim().substring(fileName.lastIndexOf(DOT) + 1)
    }
}
