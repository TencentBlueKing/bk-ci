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

package com.tencent.bkrepo.common.artifact.util

import com.tencent.bkrepo.common.api.constant.StringPool

/**
 * 包唯一id工具类
 */
object PackageKeys {

    private const val DOCKER = "docker"
    private const val NPM = "npm"
    private const val HELM = "helm"
    private const val RPM = "rpm"
    private const val PYPI = "pypi"
    private const val COMPOSER = "composer"
    private const val NUGET = "nuget"
    private const val SEPARATOR = "://"

    /**
     * 生成gav格式key
     */
    fun ofGav(groupId: String, artifactId: String): String {
        return StringBuilder("gav://").append(groupId)
            .append(StringPool.COLON)
            .append(artifactId)
            .toString()
    }

    /**
     * 生成docker格式key
     *
     * 例子: docker://test
     */
    fun ofDocker(name: String): String {
        return ofName(DOCKER, name)
    }

    /**
     * 生成npm格式key
     *
     * 例子: npm://test
     */
    fun ofNpm(name: String): String {
        return ofName(NPM, name)
    }

    /**
     * 生成npm格式key
     *
     * 例子: npm://test
     */
    fun ofHelm(name: String): String {
        return ofName(HELM, name)
    }

    /**
     * 生成rpm格式key
     * 例子: rpm://test
     */
    fun ofRpm(path: String, name: String): String {
        return if (path.isNotBlank()) {
            StringBuilder(RPM).append(SEPARATOR).append(path)
                .append(StringPool.SLASH)
                .append(name)
                .toString()
        } else {
            StringBuilder(RPM).append(SEPARATOR)
                .append(name)
                .toString()
        }
    }

    /**
     * 生成pypi格式key
     * 例子: pypi://test
     */
    fun ofPypi(name: String): String {
        return ofName(PYPI, name)
    }

    /**
     * 生成composer格式key
     * 例子: composer://test
     */
    fun ofComposer(name: String): String {
        return ofName(COMPOSER, name)
    }

    /**
     * 生成nuget格式key
     * 例子: nuget://test
     */
    fun ofNuget(name: String): String {
        return ofName(NUGET, name)
    }

    /**
     * 解析npm格式的key
     *
     * 例子: npm://test  ->  test
     */
    fun resolveNpm(npmKey: String): String {
        return resolveName(NPM, npmKey)
    }

    /**
     * 解析helm格式的key
     *
     * 例子: helm://test  ->  test
     */
    fun resolveHelm(helmKey: String): String {
        return resolveName(HELM, helmKey)
    }

    /**
     * 解析docker格式的key
     *
     * 例子: docker://test  ->  test
     */
    fun resolveDocker(dockerKey: String): String {
        return resolveName(DOCKER, dockerKey)
    }

    /**
     * 解析rpm格式的key
     * 例子: rpm://test  ->  test
     */
    fun resolveRpm(rpmKey: String): String {
        return resolveName(RPM, rpmKey)
    }

    /**
     * 解析pypi格式的key
     *
     * 例子: pypi://test  ->  test
     */
    fun resolvePypi(pypiKey: String): String {
        return resolveName(PYPI, pypiKey)
    }

    /**
     * 解析composer格式的key
     *
     * 例子: composer://test  ->  test
     */
    fun resolveComposer(composerKey: String): String {
        return resolveName(COMPOSER, composerKey)
    }

    /**
     * 生成name格式key
     *
     * 例子: {schema}://test
     */
    private fun ofName(schema: String, name: String): String {
        return StringBuilder(schema).append(SEPARATOR).append(name).toString()
    }

    /**
     * 解析nuget格式的key
     *
     * 例子: nuget://test  ->  test
     */
    fun resolveNuget(nugetKey: String): String {
        return resolveName(NUGET, nugetKey)
    }

    /**
     * 解析name格式key
     *
     * 例子: {schema}://test  ->  test
     */
    private fun resolveName(schema: String, nameKey: String): String {
        val prefix = StringBuilder(schema).append(SEPARATOR).toString()
        return nameKey.substringAfter(prefix)
    }
}
