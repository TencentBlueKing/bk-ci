package com.tencent.devops.common.pulsar.utils


import com.tencent.devops.common.pulsar.enum.Serialization
import org.apache.pulsar.client.api.Schema
import java.lang.reflect.Method

object SchemaUtils {

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
                throw IllegalArgumentException("Unknown producer schema.")
            }
        }
    }

    fun getSchema(serialisation: Serialization, clazz: Class<*>): Schema<*> {
        if (clazz == ByteArray::class.java) {
            return Schema.BYTES
        }
        return getGenericSchema(serialisation, clazz)
    }

    fun isProto(serialization: Serialization): Boolean {
        return serialization === Serialization.PROTOBUF
    }

    fun getParameterType(method: Method): Class<*> {
        return method.parameterTypes[0]
    }
}
