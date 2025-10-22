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

package com.tencent.devops.process.pojo.code.svn

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.webhook.pojo.code.svn.SvnCommitEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SvnCommitEventTest {

    @Test
    fun test() {
        val json = """
{
    "userName": "user1",
    "log": "my test",
    "rep_name": "repoGroup/repo2_proj",
    "revision": 14712,
    "eventType": 1,
    "files": [
        {
            "type": "U",
            "file": "branches/a_20200923_ChunkAst/Runtime/All/coreMem/Chunk.h",
            "isFile":1,
            "size": "12560"
        },
        {
            "type": "U",
            "file": "branches/a_20200923_ChunkAst/Runtime/All/coreMem/Impl/Chunk.cpp",
            "isFile":0,
            "size": "31390"
        }
    ],
    "paths": [
        "branches/a_20200923_ChunkAst/Runtime/All/CoreMem/",
        "branches/a_20200923_ChunkAst/Runtime/All/coreMem/Impl/"
    ],
    "commitTime": 1616925582157
}
        """
        println(json)
        val to = JsonUtil.to(json, SvnCommitEvent::class.java)
        Assertions.assertEquals(2, to.files.size)
        Assertions.assertEquals(2, to.paths.size)
        Assertions.assertTrue(to.files[0].fileFlag)
        Assertions.assertFalse(to.files[1].fileFlag)

        val objectMapper = JsonUtil.getObjectMapper()
        val svnCommitEvent = objectMapper.readValue(json, SvnCommitEvent::class.java)
        Assertions.assertTrue(svnCommitEvent.files[0].fileFlag)
        Assertions.assertEquals(2, svnCommitEvent.files.size)
        Assertions.assertEquals(2, svnCommitEvent.paths.size)
        Assertions.assertTrue(svnCommitEvent.files[0].fileFlag)
        Assertions.assertFalse(svnCommitEvent.files[1].fileFlag)
    }
}
