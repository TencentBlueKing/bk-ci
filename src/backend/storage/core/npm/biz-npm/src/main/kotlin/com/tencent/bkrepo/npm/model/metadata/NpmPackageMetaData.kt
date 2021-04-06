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

package com.tencent.bkrepo.npm.model.metadata

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable

/**
 * npm package metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "_id", "_rev", "name", "description", "dist-tags", "versions", "_attachments"
)
@JsonIgnoreProperties(ignoreUnknown = true)
class NpmPackageMetaData : Serializable {
    @JsonProperty("_id")
    var id: String? = null

    @JsonProperty("_rev")
    var rev: String = "1-0"

    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("description")
    var description: String? = null

    @JsonProperty("dist-tags")
    var distTags: DistTags = DistTags()

    @JsonProperty("versions")
    @JsonDeserialize(using = VersionsDeserializer::class)
    @JsonSerialize(using = VersionsSerializer::class)
    var versions: Versions = Versions()

    var readme: String? = null

    @JsonDeserialize(using = MaintainersDeserializer::class)
    var maintainers: JsonNode? = null

    var author: JsonNode? = null
    var time: Time = Time()
    var repository: JsonNode? = null
    var users: Users = Users()
    var readmeFilename: JsonNode? = null
    var homepage: JsonNode? = null
    var keywords: JsonNode? = null
    var contributors: JsonNode? = null
    var bugs: JsonNode? = null
    var license: JsonNode? = null

    @JsonProperty("_attachments")
    var attachments: Attachments? = null
    private val other: MutableMap<String, Any?> = mutableMapOf()

    @JsonAnySetter
    fun set(name: String, value: Any?) {
        this.other[name] = value
    }

    @JsonAnyGetter
    fun any(): Map<String, Any?> {
        return this.other
    }

    class Users : Serializable {
        private var users: MutableMap<String, Any> = mutableMapOf()

        @JsonAnySetter
        fun add(name: String, value: Any) {
            this.users[name] = value
        }

        @JsonAnyGetter
        fun getMap(): Map<String, Any> {
            return this.users
        }
    }

    class Time : Serializable {
        private var versions: MutableMap<String, String> = mutableMapOf()

        @JsonAnySetter
        fun add(name: String, value: String) {
            this.versions[name] = value
        }

        @JsonAnyGetter
        fun getMap(): MutableMap<String, String> {
            return this.versions
        }

        fun get(key: String): String {
            return this.versions[key]!!
        }
    }

    class Attachment : Serializable {
        @JsonProperty("content_type")
        var contentType: String? = null
        var data: String? = null
        var length: Int? = null
    }

    class Attachments : Serializable {
        private var tarballs: MutableMap<String, Attachment> = mutableMapOf()

        @JsonAnySetter
        fun add(name: String, value: Attachment) {
            this.tarballs[name] = value
        }

        @JsonAnyGetter
        fun getMap(): Map<String, Attachment> {
            return this.tarballs
        }
    }

    class Versions : Serializable {
        var map: MutableMap<String, NpmVersionMetadata> = mutableMapOf()
    }

    class DistTags : Serializable {
        private val tags: MutableMap<String, String> = mutableMapOf()

        @JsonAnySetter
        fun set(name: String, value: String) {
            this.tags[name] = value
        }

        @JsonAnyGetter
        fun getMap(): MutableMap<String, String> {
            return this.tags
        }
    }
}
