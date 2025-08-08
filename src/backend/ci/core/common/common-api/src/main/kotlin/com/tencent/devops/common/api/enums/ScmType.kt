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

package com.tencent.devops.common.api.enums

enum class ScmType(val alis: String) {
    CODE_SVN("svn"),
    CODE_GIT("git"),
    CODE_GITLAB("gitlab"),
    GITHUB("github"),
    CODE_TGIT("tgit"),
    CODE_P4("p4"),
    SCM_GIT("scm_git"),
    SCM_SVN("scm_svn"),
    SCM_P4("scm_p4")
    ;

    companion object {
        fun parse(type: ScmType): Short {
            return when (type) {
                CODE_SVN -> 1.toShort()
                CODE_GIT -> 2.toShort()
                CODE_GITLAB -> 3.toShort()
                GITHUB -> 4.toShort()
                CODE_TGIT -> 5.toShort()
                CODE_P4 -> 6.toShort()
                SCM_GIT -> 7.toShort()
                SCM_SVN -> 8.toShort()
                SCM_P4 -> 9.toShort()
            }
        }

        fun parse(alis: String?): ScmType? {
            if (alis.isNullOrBlank()) return null
            values().forEach {
                if (alis == it.alis) return it
            }
            return null
        }
    }
}
