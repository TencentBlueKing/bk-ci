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

package com.tencent.bkrepo.common.stream.binder.pulsar.util

import com.google.protobuf.GeneratedMessageV3
import com.tencent.bkrepo.common.stream.binder.pulsar.constant.Serialization
import com.tencent.bkrepo.common.stream.binder.pulsar.error.exception.ProducerInitException
import java.lang.reflect.Method
import org.apache.pulsar.client.api.Schema

object SchemaUtils {
    @Throws(RuntimeException::class)
    private fun <T> getGenericSchema(serialization: Serialization, clazz: Class<T>): Schema<*> {

        return when (serialization) {
            Serialization.JSON -> {
                Schema.JSON(clazz)
            }
            Serialization.AVRO -> {
                Schema.AVRO(clazz)
            }
            Serialization.STRING -> {
                Schema.STRING
            }
            else -> {
                throw ProducerInitException("Unknown producer schema.")
            }
        }
    }

    @Throws(RuntimeException::class)
    private fun <T : GeneratedMessageV3> getProtoSchema(serialization: Serialization, clazz: Class<T>): Schema<*> {
        if (serialization === Serialization.PROTOBUF) {
            return Schema.PROTOBUF(clazz)
        }
        throw ProducerInitException("Unknown producer schema.")
    }

    fun getSchema(serialisation: Serialization, classStr: String? = null): Schema<*> {
        val temp = if (classStr == null) {
            ByteArray::class.java
        } else {
            Class.forName(classStr).kotlin.java
        }
        if (temp == ByteArray::class.java) {
            return Schema.BYTES
        }
        return if (isProto(serialisation)) {
            // TODO 待处理
            getProtoSchema(serialisation, temp as Class<out GeneratedMessageV3>)
        } else {
            getGenericSchema(serialisation, temp as Class<Any>)
        }
    }

    fun isProto(serialization: Serialization): Boolean {
        return serialization === Serialization.PROTOBUF
    }

    fun getParameterType(method: Method): Class<*>? {
        return method.parameterTypes[0]
    }
}
