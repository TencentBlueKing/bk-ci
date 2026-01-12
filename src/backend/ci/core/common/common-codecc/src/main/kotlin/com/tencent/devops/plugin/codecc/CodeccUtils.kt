/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.plugin.codecc

object CodeccUtils {

    const val BK_CI_CODECC_TASK_ID = "BK_CI_CODECC_TASK_ID"

    const val BK_CI_CODECC_ATOM_ID_TO_TASK_ID = "BK_CI_CODECC_ATOM_ID_TO_TASK_ID"

    const val BK_CI_CODECC_V3_ATOM = "CodeccCheckAtomDebug"

    const val BK_CI_CODECC_COMMUNITY_ATOM = "CodeCCCheckAtom"

    const val BK_CI_CODECC_REPORT_URL = "BK_CI_CODECC_REPORT_URL"

    fun isCodeccAtom(atomName: String?): Boolean {
        return isCodeccNewAtom(atomName)
    }

    private fun isCodeccNewAtom(atomName: String?): Boolean {
        return isCodeccV2Atom(atomName) || isCodeccV3Atom(atomName) || isCodeccCommunityAtom(atomName)
    }

    private fun isCodeccV2Atom(atomName: String?): Boolean {
        return atomName == "CodeccCheckAtom"
    }

    private fun isCodeccV3Atom(atomName: String?): Boolean {
        return atomName == BK_CI_CODECC_V3_ATOM
    }

    // 社区版插件code和内网版不同
    fun isCodeccCommunityAtom(atomName: String?): Boolean {
        return atomName == BK_CI_CODECC_COMMUNITY_ATOM
    }

    // 主要是因为codecc插件版本太多，又要统一处理，故加此map
    val realAtomCodeMap = mapOf(
        "CodeccCheckAtom" to BK_CI_CODECC_V3_ATOM,
        BK_CI_CODECC_V3_ATOM to BK_CI_CODECC_V3_ATOM
    )
}
