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

package com.tencent.bkrepo.common.artifact.path

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.StringPool.DOT
import com.tencent.bkrepo.common.api.constant.StringPool.DOUBLE_DOT
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.api.constant.ensureSuffix
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode.NODE_PATH_INVALID

/**
 * 路径处理工具类
 *
 * path节点目录命名规则：以'/'开头，以'/'结尾，根路径为/
 * name节点文件命名规则：不含'/'
 * fullPath全路径命名规则：以'/'开头，结尾不含'/'，根路径为/
 */
object PathUtils {

    /**
     * 禁用文件名
     */
    private val FORBIDDEN_NAME_LIST = listOf(DOT, DOUBLE_DOT)

    /**
     * 需要正则转移的关键字
     */
    private val KEYWORD_LIST = listOf("\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|", "?", "&")

    /**
     * 最大目录深度
     */
    private const val MAX_DIR_DEPTH = 64

    /**
     * 文件名称最大长度
     */
    private const val MAX_FILENAME_LENGTH = 1024

    /**
     * unix文件分隔字符
     */
    private const val DOT_SEPARATOR = CharPool.DOT

    /**
     * windows文件分隔符
     */
    private const val WIN_SEPARATOR = CharPool.BACKSLASH

    /**
     * unix文件分隔字符
     */
    const val UNIX_SEPARATOR = CharPool.SLASH

    /**
     * 根目录
     */
    const val ROOT = StringPool.ROOT

    /**
     * 转为path格式
     *
     */
    fun toPath(input: String): String {
        return input.ensureSuffix(UNIX_SEPARATOR)
    }

    /**
     * 转为fullPath格式
     *
     */
    fun toFullPath(input: String): String {
        return if (isRoot(input)) ROOT else input.removeSuffix(StringPool.SLASH)
    }

    /**
     * 格式化目录名称, 返回格式/a/b/c/，根目录返回/
     * /a/b/c -> /a/b/c/
     * /a/b/c/ -> /a/b/c/
     *
     * 格式不正确抛[ErrorCodeException]异常
     */
    fun normalizePath(input: String): String {
        return normalizeFullPath(input).ensureSuffix(UNIX_SEPARATOR)
    }

    /**
     * 格式化全路径名称, 返回格式/a/b/c，根目录返回/
     *
     * /a/b/c -> /a/b/c
     * /a/b/c/ -> /a/b/c
     */
    @Throws(ErrorCodeException::class)
    fun normalizeFullPath(input: String): String {
        val names = mutableListOf<String>()
        input.replace(WIN_SEPARATOR, UNIX_SEPARATOR)
            .splitToSequence(UNIX_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != DOT }
            .forEach {
                if (it == DOUBLE_DOT) {
                    if (names.isNotEmpty()) {
                        names.removeAt(names.size - 1)
                    }
                    return@forEach
                }
                names.add(validateFileName(it))
            }
        val builder = StringBuilder()
        names.takeIf { it.isNotEmpty() } ?: return ROOT
        names.takeIf { it.size <= MAX_DIR_DEPTH } ?: throw ErrorCodeException(NODE_PATH_INVALID, input)
        names.forEach { builder.append(UNIX_SEPARATOR).append(it) }
        return builder.toString()
    }

    /**
     * 验证文件名称，返回格式abc.txt
     * 不能包含/，不能全为空，不能超过指定长度
     */
    fun validateFileName(input: String): String {
        try {
            require(input.isNotBlank())
            require(input.length <= MAX_FILENAME_LENGTH)
            require(!FORBIDDEN_NAME_LIST.contains(input))
            require(!input.contains(UNIX_SEPARATOR))
            require(!input.contains(WIN_SEPARATOR))
            checkIllegalByte(input)
        } catch (exception: IllegalArgumentException) {
            throw ErrorCodeException(NODE_PATH_INVALID, input)
        }
        return input
    }

    /**
     * 根据路径列表和文件名组合全路径，返回格式/a/b/c/abc.txt
     *
     * /a/b/c + d -> /a/b/c/d
     * /a/b/c/ + d -> /a/b/c/d
     */
    fun combineFullPath(path: String, name: String): String {
        return path.ensureSuffix(UNIX_SEPARATOR).plus(name.trimStart(UNIX_SEPARATOR))
    }

    /**
     * 根据路径列表和文件名组合新的路径，返回格式/a/b/c/
     *
     * /a/b/c + d -> /a/b/c/d/
     * /a/b/c/ + d -> /a/b/c/d/
     */
    fun combinePath(parent: String, name: String): String {
        return parent.ensureSuffix(UNIX_SEPARATOR)
            .plus(name.trimStart(UNIX_SEPARATOR))
            .ensureSuffix(UNIX_SEPARATOR)
    }

    /**
     * 根据fullPath解析目录名称, 返回格式/a/b/c/
     *
     * /a/b/c -> /a/b/
     * /a/b/c/ -> /a/b/
     */
    fun resolveParent(fullPath: String): String {
        val index = fullPath.trimEnd(UNIX_SEPARATOR).lastIndexOf(UNIX_SEPARATOR)
        return if (isRoot(fullPath) || index <= 0) ROOT else fullPath.substring(0, index + 1)
    }

    /**
     * 根据fullPath解析文件名称，返回格式abc.txt
     *
     * /a/b/c -> c
     * /a/b/c/ -> c
     * / -> ""
     */
    fun resolveName(fullPath: String): String {
        val trimmedPath = fullPath.trimEnd(UNIX_SEPARATOR)
        return if (isRoot(trimmedPath)) {
            StringPool.EMPTY
        } else {
            trimmedPath.substring(trimmedPath.lastIndexOf(UNIX_SEPARATOR) + 1)
        }
    }

    /**
     * 解析文件后缀
     */
    fun resolveExtension(fileName: String): String {
        return fileName.trim().substring(fileName.lastIndexOf(DOT_SEPARATOR) + 1)
    }

    /**
     * 判断路径是否为根目录
     */
    fun isRoot(path: String): Boolean {
        return path == ROOT || path.isBlank()
    }

    /**
     * 判断路径[path]是否为[parent]的子目录
     */
    fun isSubPath(path: String, parent: String): Boolean {
        val formatParent = parent.ensurePrefix(UNIX_SEPARATOR)
        return path.startsWith(formatParent)
    }

    /**
     * 正则特殊符号转义
     */
    fun escapeRegex(input: String): String {
        var escapedString = input.trim()
        if (escapedString.isNotBlank()) {
            KEYWORD_LIST.forEach {
                if (escapedString.contains(it)) {
                    escapedString = escapedString.replace(it, "\\$it")
                }
            }
        }
        return escapedString
    }

    /**
     * 检查非法字符
     */
    private fun checkIllegalByte(name: String) {
        for (char in name) {
            require(char.toInt() != 0)
        }
    }
}
