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

package com.tencent.devops.store.pojo.common.enums

enum class ReleaseTypeEnum(val releaseType: Int) {
    NEW(0), // 新上架
    INCOMPATIBILITY_UPGRADE(1), // 非兼容性升级
    COMPATIBILITY_UPGRADE(2), // 兼容性功能更新
    COMPATIBILITY_FIX(3), // 兼容性问题修正
    CANCEL_RE_RELEASE(4), // 取消发布后重新发布
    HIS_VERSION_UPGRADE(5), // 历史大版本下的小版本更新
    BRANCH_TEST(6); // 分支测试

    fun isDefaultShow(): Boolean = this == COMPATIBILITY_UPGRADE || this == COMPATIBILITY_FIX

    companion object {

        fun getReleaseTypeObj(releaseType: Int): ReleaseTypeEnum? {
            values().forEach { enumObj ->
                if (enumObj.releaseType == releaseType) {
                    return enumObj
                }
            }
            return null
        }

        fun getReleaseType(releaseType: Int): String {
            values().forEach { enumObj ->
                if (enumObj.releaseType == releaseType) {
                    return enumObj.name
                }
            }
            return NEW.name
        }
    }
}
