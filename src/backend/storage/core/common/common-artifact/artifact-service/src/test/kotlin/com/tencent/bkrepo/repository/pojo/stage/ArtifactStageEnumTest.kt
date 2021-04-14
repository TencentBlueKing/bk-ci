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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ArtifactStageEnumTest {

    @Test
    @DisplayName("测试nextStage")
    fun testNextStage() {
        Assertions.assertEquals(ArtifactStageEnum.PRE_RELEASE, ArtifactStageEnum.NONE.nextStage())
        Assertions.assertEquals(ArtifactStageEnum.RELEASE, ArtifactStageEnum.PRE_RELEASE.nextStage())
        assertThrows<IllegalArgumentException> { ArtifactStageEnum.RELEASE.nextStage() }
    }

    @Test
    @DisplayName("测试upgrade")
    fun testUpgrade() {
        Assertions.assertEquals(
            ArtifactStageEnum.PRE_RELEASE,
            ArtifactStageEnum.NONE.upgrade(ArtifactStageEnum.PRE_RELEASE)
        )
        Assertions.assertEquals(ArtifactStageEnum.RELEASE, ArtifactStageEnum.NONE.upgrade(ArtifactStageEnum.RELEASE))
        Assertions.assertEquals(
            ArtifactStageEnum.RELEASE,
            ArtifactStageEnum.PRE_RELEASE.upgrade(ArtifactStageEnum.RELEASE)
        )

        assertThrows<IllegalArgumentException> { ArtifactStageEnum.NONE.upgrade(ArtifactStageEnum.NONE) }
        assertThrows<IllegalArgumentException> { ArtifactStageEnum.RELEASE.upgrade(ArtifactStageEnum.PRE_RELEASE) }
        assertThrows<IllegalArgumentException> { ArtifactStageEnum.RELEASE.upgrade(ArtifactStageEnum.RELEASE) }
    }

    @Test
    @DisplayName("测试ofTagOrDefault")
    fun testOfTagOrDefault() {
        Assertions.assertEquals(ArtifactStageEnum.NONE, ArtifactStageEnum.ofTagOrDefault(""))
        Assertions.assertEquals(ArtifactStageEnum.NONE, ArtifactStageEnum.ofTagOrDefault("  "))
        Assertions.assertEquals(ArtifactStageEnum.NONE, ArtifactStageEnum.ofTagOrDefault(null))
        Assertions.assertEquals(ArtifactStageEnum.RELEASE, ArtifactStageEnum.ofTagOrDefault("@release"))
        Assertions.assertEquals(ArtifactStageEnum.NONE, ArtifactStageEnum.ofTagOrDefault("xxx"))
    }

    @Test
    @DisplayName("测试ofTagOrNull")
    fun testOfTagOrNull() {
        Assertions.assertNull(ArtifactStageEnum.ofTagOrNull(""))
        Assertions.assertNull(ArtifactStageEnum.ofTagOrNull("  "))
        Assertions.assertNull(ArtifactStageEnum.ofTagOrNull(null))
        Assertions.assertEquals(ArtifactStageEnum.RELEASE, ArtifactStageEnum.ofTagOrNull("@release"))
        assertThrows<IllegalArgumentException> { ArtifactStageEnum.ofTagOrNull("xxx") }
    }
}
