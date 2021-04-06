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

package com.tencent.bkrepo.docker.manifest

import com.tencent.bkrepo.docker.model.DockerDigest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ManifestSchema2DeserializerTest {

    @Test
    @DisplayName("测试schema v2协议解析")
    fun manifestSchema2Test() {
        val manifest =
            """{
   "schemaVersion": 2,
   "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
   "config": {
      "mediaType": "application/vnd.docker.container.image.v1+json",
      "size": 7645,
      "digest": "sha256:e2b047b17a138636a6ba9abd71c6e2e99ff38a5110177d2a34557eff61e0b040"
   },
   "layers": [
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 27098756,
         "digest": "sha256:afb6ec6fdc1c3ba04f7a56db32c5ff5ff38962dc4cd0ffdef5beaa0ce2eb77e2"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 26201443,
         "digest": "sha256:2e231683bfde7f6cdb860dcaf855c8aaf8a4cdb83ab8dd345ab35e8d5a2e421a"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 202,
         "digest": "sha256:511e2efefada0b20168024e58d73b3c5dcaa71383f77461c4be74d742838f5df"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 635,
         "digest": "sha256:e8fd0ec105c9a0ba769b2357195c0842fddf670c55f9e6a4a5f55d6fa61311d5"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 1256153,
         "digest": "sha256:97357440e4e4b86c59294355fd3747ec274866b3028f9962d16ee131e8bdb0db"
      }
   ]
}"""

        val configFile = ""
        val manifestByte = manifest.toByteArray()
        val configByte = configFile.toByteArray()
        val dockerRepo = "nginx"
        val tag = "latest"
        val digest = DockerDigest("sha256:18f43d8df2d31b43bb904aa3c1ae463ca03855f34700ea85bbf17b4b090a9b75")
        val manifestMetaData =
            ManifestSchema2Deserializer.deserialize(manifestByte, configByte, dockerRepo, tag, digest)
        Assertions.assertNotEquals(manifestMetaData.blobsInfo.size, 0)
        Assertions.assertNotEquals(manifestMetaData.tagInfo.totalSize, 0)
        Assertions.assertEquals(manifestMetaData.tagInfo.labels.size(), 0)
    }
}
