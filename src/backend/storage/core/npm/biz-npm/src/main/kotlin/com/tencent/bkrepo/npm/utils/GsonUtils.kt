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

package com.tencent.bkrepo.npm.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object GsonUtils {
    val gson: Gson =
        GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun gsonToInputStream(obj: JsonElement): InputStream {
        return gson.toJson(obj).byteInputStream()
    }

    fun stringToArray(gsonString: String): JsonArray {
        return gson.fromJson(gsonString, JsonArray::class.java)
    }

    fun <T> gsonToMaps(gsonString: String): Map<String, T>? {
        return gson.fromJson(gsonString, object : TypeToken<Map<String, T>>() {}.type)
    }

    fun <T> gsonToMaps(gsonString: JsonElement): Map<String, T>? {
        return gson.fromJson(gsonString, object : TypeToken<Map<String, T>>() {}.type)
    }

    fun <T> gsonToList(gsonString: String): List<T> {
        return gson.fromJson(gsonString, object : TypeToken<List<T>>() {}.type)
    }

    fun <T> parseJsonArrayToList(jsonArray: String?): List<T> {
        return jsonArray?.let { gsonToList<T>(it) } ?: emptyList()
    }

    fun <T> gsonToBean(gsonString: String, cls: Class<T>): T? {
        return gson.fromJson(gsonString, cls)
    }

    fun <T> mapToGson(map: Map<String, T>): JsonObject {
        return JsonParser.parseString(gson.toJson(map)).asJsonObject
    }

    fun transferFileToJson(file: File): JsonObject {
        return gson.fromJson<JsonObject>(
            InputStreamReader(file.inputStream()),
            object : TypeToken<JsonObject>() {}.type
        )
    }

    fun transferInputStreamToJson(inputStream: InputStream): JsonObject {
        return gson.fromJson<JsonObject>(
            InputStreamReader(inputStream),
            object : TypeToken<JsonObject>() {}.type
        )
    }
}
