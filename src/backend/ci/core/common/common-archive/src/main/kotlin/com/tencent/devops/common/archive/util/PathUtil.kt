/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.common.archive.util

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil

object PathUtil {
    fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    fun isFolder(path: String): Boolean {
        return path.endsWith("/")
    }

    fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    /**
     * 规范化并校验仓库/归档相对路径（bkrepo destPath、下载 filePath 等）。
     *
     * 必须先反复 URL 解码再规范化：contains("../") 这类黑名单会被 %2e%2e%2f、%252e 绕过。
     * 再按 POSIX 规则展开 . / ..，拒绝越出虚拟根、Windows 盘符和空字节。
     * 不使用本机 Paths.get：Windows 上会把 / 变成 \，且盘符语义与仓库路径不一致。
     *
     */
    fun getNormalizedPath(filePath: String): String = normalizeAndValidateRepoPath(filePath)

    fun normalizeAndValidateRepoPath(filePath: String, requiredPrefix: String? = null): String {
        if (filePath.isBlank() || filePath.contains('\u0000')) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath)
            )
        }
        // 先解码再规范化，避免二次编码绕过后续的 .. 检查
        val decoded = FileUtil.decodeUrlRepeatedly(filePath)
        val unified = decoded.replace('\\', '/')
        val keepTrailingSlash = unified.length > 1 && unified.endsWith('/')
        if (isWindowsAbsolute(unified)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath)
            )
        }
        val normalized = normalizeUnixPath(unified)
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        // 规范化后仍残留 .. 说明已越出虚拟根（例如 ../evil）
        if (normalized.isBlank() || normalized == "/" || segments.contains("..")) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(filePath)
            )
        }
        if (requiredPrefix != null) {
            val prefix = requiredPrefix.replace('\\', '/').trim('/')
            val relative = normalized.trim('/')
            // 前缀校验必须按路径段对齐，避免 /data 误匹配 /data_evil
            if (relative != prefix && !relative.startsWith("$prefix/")) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(filePath)
                )
            }
        }
        // 规范化会丢掉末尾 /，自定义仓库「上传到目录」依赖这个语义，需要补回
        return if (keepTrailingSlash && !normalized.endsWith('/')) "$normalized/" else normalized
    }

    private fun isWindowsAbsolute(path: String): Boolean {
        // 只拦 C:/、C:\，不拦 C:foo / v:1.0 这类仓库内合法段
        return path.length >= 3 &&
            path[0].isLetter() &&
            path[1] == ':' &&
            (path[2] == '/' || path[2] == '\\')
    }

    /**
     * 按 POSIX 规则展开路径，不调用本机 Paths.get。
     * Windows 上 Paths.get 会把 / 变成 \，且盘符语义与 bkrepo 仓库路径不一致。
     */
    private fun normalizeUnixPath(path: String): String {
        val absolute = path.startsWith('/')
        val parts = mutableListOf<String>()
        for (seg in path.split('/')) {
            when {
                // 连续斜杠产生空段，"." 表示当前目录，两者都不进入结果
                seg.isEmpty() || seg == "." -> Unit
                seg == ".." -> {
                    if (parts.isNotEmpty()) {
                        // 有上级段时就地弹出，例如 a/b/../c → a/c
                        parts.removeAt(parts.lastIndex)
                    } else if (!absolute) {
                        // 相对路径越出起点时保留 ..，交给调用方判定非法
                        parts.add("..")
                    }
                    // 绝对路径在根上再 .. 直接丢弃，避免 /../evil 变成 ../evil 后逃出虚拟根
                }
                else -> parts.add(seg)
            }
        }
        val body = parts.joinToString("/")
        return if (absolute) "/$body" else body
    }
}
