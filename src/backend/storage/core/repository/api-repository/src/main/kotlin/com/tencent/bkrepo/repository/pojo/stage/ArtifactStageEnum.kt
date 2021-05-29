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

package com.tencent.bkrepo.repository.pojo.stage

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.ensurePrefix

/**
 * 制品晋级阶段枚举类
 */
enum class ArtifactStageEnum(
    val tag: String
) {

    NONE(""),

    /**
     * 预发布
     */
    PRE_RELEASE("@prerelease"),

    /**
     * 发布
     */
    RELEASE("@release");

    /**
     * 晋级
     */
    @Throws(IllegalArgumentException::class)
    fun upgrade(toStage: ArtifactStageEnum?): ArtifactStageEnum {
        val newStage = toStage ?: this.nextStage()
        require(newStage.ordinal > this.ordinal) { "Illegal stage" }
        return newStage
    }

    /**
     * 下一个阶段
     */
    fun nextStage(): ArtifactStageEnum {
        require(this.ordinal < values().size - 1) { "Illegal stage" }
        return values()[this.ordinal + 1]
    }

    companion object {
        /**
         * 根据[tag]反查[ArtifactStageEnum]
         *
         * 当找不到[tag]对应的值时返回[NONE]
         */
        @Throws(IllegalArgumentException::class)
        fun ofTagOrDefault(tag: String?): ArtifactStageEnum {
            if (tag.isNullOrBlank()) {
                return NONE
            }
            val normalizedTag = tag.ensurePrefix(CharPool.AT).toLowerCase()
            return values().find { stage -> stage.tag == normalizedTag } ?: NONE
        }

        /**
         * 根据[tag]反查[ArtifactStageEnum]
         *
         * [tag]为`null`则返回`null`
         * [tag]不存在抛[IllegalArgumentException]
         */
        @Throws(IllegalArgumentException::class)
        fun ofTagOrNull(tag: String?): ArtifactStageEnum? {
            if (tag.isNullOrBlank()) {
                return null
            }
            return values().find { stage -> stage.tag == tag } ?: throw IllegalArgumentException("Unknown tag")
        }
    }
}
