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

package com.tencent.bkrepo.common.artifact.util.version

import kotlin.math.min
import kotlin.math.pow

/**
 * 语义化版本
 */
data class SemVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) : Comparable<SemVersion> {

    /**
     * Build the version name string.
     * @return version name string in Semantic Versioning 2.0.0 specification.
     */
    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (preRelease != null) {
            append('-')
            append(preRelease)
        }
        if (buildMetadata != null) {
            append('+')
            append(buildMetadata)
        }
    }

    /**
     * Calculate the version ordinal.
     */
    fun ordinal(maxDigitsPerComponent: Int = 4): Long {
        require(maxDigitsPerComponent > 0)
        val unit = BASE.pow(maxDigitsPerComponent).toInt()
        val max = unit - 1
        var ordinal = 0L
        ordinal += min(major, max)
        ordinal *= unit
        ordinal += min(minor, max)
        ordinal *= unit
        ordinal += min(patch, max)
        ordinal *= unit
        ordinal += if (preRelease == null) max else 0
        return ordinal
    }

    override fun compareTo(other: SemVersion): Int {
        var result = major - other.major
        if (result == 0) {
            result = minor - other.minor
        }
        if (result == 0) {
            result = patch - other.patch
        }
        if (result == 0) {
            result = preRelease.orEmpty().compareTo(other.preRelease.orEmpty())
        }
        if (result == 0) {
            result = buildMetadata.orEmpty().compareTo(other.buildMetadata.orEmpty())
        }
        return result
    }

    companion object {

        private const val BASE = 10.0

        /**
         * Parse the version string to [SemVersion] data object.
         * @param version version string.
         * @throws IllegalArgumentException if the version is not valid.
         */
        fun parse(version: String): SemVersion {
            return SemVersionParser.parse(version)
        }

        /**
         * Validate the [version] string is a valid SemVer.
         *
         */
        fun validate(version: String): Boolean {
            return try {
                parse(version)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
