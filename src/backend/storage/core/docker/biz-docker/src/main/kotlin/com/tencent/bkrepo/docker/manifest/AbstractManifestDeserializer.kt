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

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.bkrepo.docker.model.DockerImageMetadata
import com.tencent.bkrepo.docker.model.ManifestMetadata

/**
 * manifest utility with each schema
 */
abstract class AbstractManifestDeserializer {

    fun populatePorts(manifestMetadata: ManifestMetadata, dockerMetadata: DockerImageMetadata) {
        dockerMetadata.config?.let {
            addPorts(manifestMetadata, dockerMetadata.config!!.exposedPorts)
        }

        dockerMetadata.containerConfig?.let {
            addPorts(manifestMetadata, dockerMetadata.containerConfig!!.exposedPorts)
        }
    }

    fun populateVolumes(manifestMetadata: ManifestMetadata, dockerMetadata: DockerImageMetadata) {
        dockerMetadata.config?.let {
            addVolumes(manifestMetadata, dockerMetadata.config!!.volumes)
        }

        dockerMetadata.containerConfig?.let {
            addVolumes(manifestMetadata, dockerMetadata.containerConfig!!.volumes)
        }
    }

    fun populateLabels(manifestMetadata: ManifestMetadata, dockerMetadata: DockerImageMetadata) {
        dockerMetadata.config?.let {
            addLabels(manifestMetadata, dockerMetadata.config!!.labels)
        }

        dockerMetadata.containerConfig?.let {
            addLabels(manifestMetadata, dockerMetadata.containerConfig!!.labels)
        }
    }

    private fun addPorts(manifestMetadata: ManifestMetadata, exposedPorts: JsonNode?) {
        exposedPorts?.let {
            val iterPorts = exposedPorts.fieldNames()
            while (iterPorts.hasNext()) {
                manifestMetadata.tagInfo.ports.add(iterPorts.next())
            }
        }
    }

    private fun addVolumes(manifestMetadata: ManifestMetadata, volumes: JsonNode?) {
        volumes?.let {
            val iterVolume = volumes.fieldNames()
            while (iterVolume.hasNext()) {
                manifestMetadata.tagInfo.volumes.add(iterVolume.next())
            }
        }
    }

    private fun addLabels(manifestMetadata: ManifestMetadata, labels: Map<String, String>?) {
        labels?.let {
            val iter = labels.entries.iterator()
            while (iter.hasNext()) {
                val label = iter.next()
                manifestMetadata.tagInfo.labels.put(label.key, label.value)
            }
        }
    }
}
