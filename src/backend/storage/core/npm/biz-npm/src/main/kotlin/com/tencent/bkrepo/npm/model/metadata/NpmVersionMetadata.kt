/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.model.metadata

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.io.IOException
import java.io.Serializable

/**
 * npm version metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class NpmVersionMetadata : Serializable {
    var name: String? = null
    var description: String? = null
    var tags: JsonNode? = null
    var version: String? = null
    var author: JsonNode? = null
    var homepage: JsonNode? = null
    var repository: JsonNode? = null
    var contributors: JsonNode? = null
    var bugs: JsonNode? = null
    var engines: JsonNode? = null
    var main: JsonNode? = null

    @JsonProperty("_id")
    var id: String? = null

    @JsonProperty("dist")
    var dist: Dist? = null
    var dependencies: Map<String, String>? = null
        set(dependencies) {
            field = if (dependencies is Map) {
                dependencies
            } else {
                if ((dependencies !is List<*> || (dependencies as List<*>).isNotEmpty()) && dependencies != null) {
                    throw IOException("Invalid package.json format. The dependencies field cannot be parsed.")
                }
                mutableMapOf()
            }
        }
    var optionalDependencies: Map<String, String>? = null
    var devDependencies: Map<String, String>? = null
        set(devDependencies) {
            field = if (devDependencies is Map) {
                devDependencies
            } else {
                if ((
                            devDependencies !is List<*> ||
                                    (devDependencies as List<*>).isNotEmpty()
                            ) && devDependencies != null
                ) {
                    throw IOException("Invalid package.json format. The devDependencies field cannot be parsed.")
                }
                mutableMapOf()
            }
        }
    var bundledDependencies: JsonNode? = null
    var peerDependencies: JsonNode? = null
    var keywords: JsonNode? = null
    var license: JsonNode? = null
    var licenses: JsonNode? = null
    var files: JsonNode? = null
    var bin: JsonNode? = null
    var man: JsonNode? = null
    var maintainers: JsonNode? = null
    var directories: JsonNode? = null
    var scripts: JsonNode? = null
    var config: JsonNode? = null
    var engineStrict: JsonNode? = null
    var os: JsonNode? = null
    var cpu: JsonNode? = null
    var preferGlobal: JsonNode? = null

    @JsonProperty("private")
    var privatePackage: JsonNode? = null
    var publishConfig: JsonNode? = null
    @JsonProperty("_from")
    var from: JsonNode? = null
    @JsonProperty("_npmVersion")
    var npmVersion: JsonNode? = null
    @JsonProperty("_npmUser")
    var npmUser: JsonNode? = null

    @JsonIgnore
    var lastModified: Long = 0
    private var other: MutableMap<String, Any?> = mutableMapOf()

    @JsonAnySetter
    fun set(name: String, value: Any?) {
        this.other[name] = value
    }

    @JsonAnyGetter
    fun any(): Map<String, Any?> {
        return this.other
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Dist : Serializable {
        @JsonProperty("tarball")
        var tarball: String? = null

        @JsonProperty("shasum")
        var shasum: String? = null

        private var other: MutableMap<String, Any?> = mutableMapOf()

        @JsonAnySetter
        fun set(name: String, value: Any?) {
            this.other[name] = value
        }

        @JsonAnyGetter
        fun any(): Map<String, Any?> {
            return this.other
        }
    }
}
