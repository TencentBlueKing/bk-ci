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

package com.tencent.devops.project.pojo

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProjectPropertiesTest {

    @Test
    fun `enableShareArtifact default value should be true`() {
        val properties = ProjectProperties()
        assertEquals(true, properties.enableShareArtifact)
    }

    @Test
    fun `enableShareArtifact can be set to false`() {
        val properties = ProjectProperties(enableShareArtifact = false)
        assertEquals(false, properties.enableShareArtifact)
    }

    @Test
    fun `enableShareArtifact can be set to null`() {
        val properties = ProjectProperties(enableShareArtifact = null)
        assertNull(properties.enableShareArtifact)
    }

    @Test
    fun `userCopy should copy enableShareArtifact`() {
        val original = ProjectProperties(enableShareArtifact = true)
        val update = ProjectProperties(enableShareArtifact = false)
        val result = original.userCopy(update)
        assertEquals(false, result.enableShareArtifact)
    }

    @Test
    fun `userCopy should preserve enableShareArtifact when update is null`() {
        val original = ProjectProperties(enableShareArtifact = false)
        val update = ProjectProperties(enableShareArtifact = null)
        val result = original.userCopy(update)
        assertNull(result.enableShareArtifact)
    }

    @Test
    fun `enableShareArtifact should serialize and deserialize correctly`() {
        val properties = ProjectProperties(enableShareArtifact = false)
        val json = JsonUtil.toJson(properties)
        val deserialized = JsonUtil.to(json, object : TypeReference<ProjectProperties>() {})
        assertEquals(false, deserialized.enableShareArtifact)
    }

    @Test
    fun `enableShareArtifact default should serialize and deserialize correctly`() {
        val properties = ProjectProperties()
        val json = JsonUtil.toJson(properties)
        val deserialized = JsonUtil.to(json, object : TypeReference<ProjectProperties>() {})
        assertEquals(true, deserialized.enableShareArtifact)
    }
}
