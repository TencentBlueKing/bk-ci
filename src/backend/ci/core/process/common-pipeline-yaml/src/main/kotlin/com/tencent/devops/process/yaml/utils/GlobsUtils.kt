/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.tencent.devops.process.yaml.utils

import java.util.regex.PatternSyntaxException

@Suppress("ComplexCondition")
object GlobsUtils {
    private const val regexMetaChars = ".^$+{[]|()"
    private const val globMetaChars = "\\*?[{"
    private fun isRegexMeta(c: Char): Boolean {
        return regexMetaChars.indexOf(c) != -1
    }

    private fun isGlobMeta(c: Char): Boolean {
        return globMetaChars.indexOf(c) != -1
    }

    private const val EOL = 0 // TBD
        .toChar()

    private fun next(glob: String, i: Int): Char {
        return if (i < glob.length) {
            glob[i]
        } else EOL
    }

    /**
     * Creates a regex pattern from the given glob expression.
     *
     * @throws PatternSyntaxException
     */
    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun toRegexPattern(globPattern: String, isDos: Boolean): String {
        var inGroup = false
        val regex = StringBuilder("^")
        var i = 0
        while (i < globPattern.length) {
            var c = globPattern[i++]
            when (c) {
                '\\' -> {
                    // escape special characters
                    if (i == globPattern.length) {
                        throw PatternSyntaxException(
                            "No character to escape",
                            globPattern, i - 1
                        )
                    }
                    val next = globPattern[i++]
                    if (isGlobMeta(next) || isRegexMeta(next)) {
                        regex.append('\\')
                    }
                    regex.append(next)
                }
                '/' -> if (isDos) {
                    regex.append("\\\\")
                } else {
                    regex.append(c)
                }
                '[' -> {
                    // don't match name separator in class
                    if (isDos) {
                        regex.append("[[^\\\\]&&[")
                    } else {
                        regex.append("[[^/]&&[")
                    }
                    if (next(globPattern, i) == '^') {
                        // escape the regex negation char if it appears
                        regex.append("\\^")
                        i++
                    } else {
                        // negation
                        if (next(globPattern, i) == '!') {
                            regex.append('^')
                            i++
                        }
                        // hyphen allowed at start
                        if (next(globPattern, i) == '-') {
                            regex.append('-')
                            i++
                        }
                    }
                    var hasRangeStart = false
                    var last = 0.toChar()
                    while (i < globPattern.length) {
                        c = globPattern[i++]
                        if (c == ']') {
                            break
                        }
                        if (c == '/' || isDos && c == '\\') {
                            throw PatternSyntaxException(
                                "Explicit 'name separator' in class",
                                globPattern, i - 1
                            )
                        }
                        // TBD: how to specify ']' in a class?
                        if (c == '\\' || c == '[' || c == '&' && next(globPattern, i) == '&') {
                            // escape '\', '[' or "&&" for regex class
                            regex.append('\\')
                        }
                        regex.append(c)
                        if (c == '-') {
                            if (!hasRangeStart) {
                                throw PatternSyntaxException(
                                    "Invalid range",
                                    globPattern, i - 1
                                )
                            }
                            if (next(globPattern, i++).also { c = it } == EOL || c == ']') {
                                break
                            }
                            if (c < last) {
                                throw PatternSyntaxException(
                                    "Invalid range",
                                    globPattern, i - 3
                                )
                            }
                            regex.append(c)
                            hasRangeStart = false
                        } else {
                            hasRangeStart = true
                            last = c
                        }
                    }
                    if (c != ']') {
                        throw PatternSyntaxException("Missing ']", globPattern, i - 1)
                    }
                    regex.append("]]")
                }
                '{' -> {
                    if (inGroup) {
                        throw PatternSyntaxException(
                            "Cannot nest groups",
                            globPattern, i - 1
                        )
                    }
                    regex.append("(?:(?:")
                    inGroup = true
                }
                '}' -> if (inGroup) {
                    regex.append("))")
                    inGroup = false
                } else {
                    regex.append('}')
                }
                ',' -> if (inGroup) {
                    regex.append(")|(?:")
                } else {
                    regex.append(',')
                }
                '*' -> if (next(globPattern, i) == '*') {
                    // crosses directory boundaries
                    regex.append(".*")
                    i++
                } else {
                    // within directory boundary
                    if (isDos) {
                        regex.append("[^\\\\]*")
                    } else {
                        regex.append("[^/]*")
                    }
                }
                '?' -> if (isDos) {
                    regex.append("[^\\\\]")
                } else {
                    regex.append("[^/]")
                }
                else -> {
                    if (isRegexMeta(c)) {
                        regex.append('\\')
                    }
                    regex.append(c)
                }
            }
        }
        if (inGroup) {
            throw PatternSyntaxException("Missing '}", globPattern, i - 1)
        }
        return regex.append('$').toString()
    }

    fun toUnixRegexPattern(globPattern: String): String {
        return toRegexPattern(globPattern, false)
    }

    fun toWindowsRegexPattern(globPattern: String): String {
        return toRegexPattern(globPattern, true)
    }
}
