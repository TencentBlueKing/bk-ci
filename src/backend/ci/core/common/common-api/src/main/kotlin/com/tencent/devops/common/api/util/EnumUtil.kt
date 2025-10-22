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

package com.tencent.devops.common.api.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Unsafe
import sun.reflect.ReflectionFactory
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import kotlin.reflect.full.isSubclassOf

/**
 * 枚举工具类，用于动态修改枚举值和内容
 *
 */
@Suppress("ALL")
object EnumUtil {

    /**
     * 动态修改枚举，但动态范围仅限于后来使用枚举类的values或valueOf方法实例化出来的枚举值
     *
     * 不支持动态替换：直接在代码中使用 MyEnum.APPLE.type 这种写法在编译期已经生成为常量，就无法实现动态替换了.
     * 支持动态替换：  MyEnum.valueOf("APPLE") MyEnum.values() 这种可以实现动态读取到
     *
     * @param <T> 枚举的泛型
     * @param enumType 要动态修改的枚举类
     * @param enumName 枚举值名称
     * @param additionalValues 枚举定义的其他字段的值，如果有，如果无传空数组
    </T> */
    inline fun <reified T : Any> addEnum(enumType: Class<T>, enumName: String, additionalValues: Array<out Any>) {

        // 检查是否是枚举类型，如果不是则抛出异常
        if (!Enum::class.java.isAssignableFrom(enumType)) {
            throw RuntimeException("class $enumType is not an instance of Enum")
        }
        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
        var valuesField: Field? = null
        val fields: Array<Field> = enumType.declaredFields
        for (field in fields) {
            if (field.name.contains("\$VALUES")) {
                valuesField = field
                break
            }
        }

        val additionalTypes = mutableListOf<Class<out Any>>()
        additionalValues.forEach { value ->
            when {
                // kotlin 中 List可变mutable与不可变immutable是两个不同的接口，需要区分对待
                value::class.isSubclassOf(MutableList::class) -> additionalTypes.add(MutableList::class.java)
                // 赋值时注意其他Java对List的各种扩展子类，都转为List
                value::class.isSubclassOf(List::class) -> additionalTypes.add(List::class.java)
                // 其他场景暂时未覆盖完
                else -> additionalTypes.add(value::class.java)
            }
        }

        AccessibleObject.setAccessible(arrayOf(valuesField), true)

        try {
            // 将先之前的枚举值保存下来
            val previousValues = valuesField!![enumType] as Array<T>

            val values: MutableList<T> = mutableListOf()
            var ordinal = previousValues.size

            previousValues.forEachIndexed { idx, value ->
                // 如果是存在的枚举值，则服它替换旧值
                if (value.toString() == enumName) {
                    ordinal = idx
                }
                values.add(value)
            }

//            // 构建新枚举值
//            val newValue: T = makeEnum(
//                enumClass = enumType,
//                value = enumName,
//                ordinal = ordinal,
//                additionalTypes = additionalTypes.toTypedArray(),
//                additionalValues = additionalValues
//            )

            // 构造新的枚举实例
            val newValue = makeEnum(enumType, enumName, ordinal, additionalValues)

            if (ordinal < previousValues.size) {
                values[ordinal] = newValue
            } else {
                values.add(newValue)
            }

            setStaticFieldValue(enumType, valuesField, values.toTypedArray())

            cleanEnumCache(enumType)
        } catch (e: Exception) {
            println(e)
            e.printStackTrace()
            throw RuntimeException(e.message, e)
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    inline fun <reified T : Any> setStaticFieldValue(
        enumType: Class<T>,
        valuesField: Field,
        newValues: Any?
    ) {
        val unsafe = getUnsafe()
        val arrayBaseOffset = unsafe.staticFieldOffset(valuesField)
        unsafe.putObjectVolatile(enumType, arrayBaseOffset, newValues)
    }

    inline fun <reified T : Any> setObjectFieldValue(
        enumType: Class<T>,
        valuesField: Field,
        newValues: Any?
    ) {
        val unsafe = getUnsafe()
        val arrayBaseOffset = unsafe.objectFieldOffset(valuesField)
        unsafe.putObjectVolatile(enumType, arrayBaseOffset, newValues)
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    inline fun <reified T : Any> blankField(enumClass: Class<T>, fieldName: String) {
        for (field in Class::class.java.declaredFields) {
            if (field.name.contains(fieldName)) {
                setObjectFieldValue(enumClass, field, null)
                break
            }
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    inline fun <reified T : Any> cleanEnumCache(enumClass: Class<T>) {
        blankField(enumClass, "enumConstantDirectory") // OracleJDK & OpenJDK
        blankField(enumClass, "enumConstants") // IBM JDK
    }

    fun compareParameterType(constructorParameterType: Array<Class<*>>, parameterTypes: Array<Class<*>?>): Boolean {
        if (constructorParameterType.size != parameterTypes.size) {
            return false
        }
        for (i in constructorParameterType.indices) {
            if (constructorParameterType[i] !== parameterTypes[i]) {
                if (constructorParameterType[i].isPrimitive && parameterTypes[i]!!.isPrimitive) {
                    if (constructorParameterType[i].kotlin.javaPrimitiveType
                        !== parameterTypes[i]!!.kotlin.javaPrimitiveType
                    ) {
                        return false
                    }
                }
            }
        }
        return true
    }

//    @Throws(Exception::class)
//    inline fun <reified T : Any> makeEnum(
//        enumClass: Class<T>,
//        value: String,
//        ordinal: Int,
//        additionalTypes: Array<Class<out Any>>,
//        additionalValues: Array<out Any>
//    ): T {
//        val params = arrayOfNulls<Any>(additionalValues.size + 2)
//        params[0] = value
//        params[1] = Integer.valueOf(ordinal)
//        System.arraycopy(additionalValues, 0, params, 2, additionalValues.size)
//        return enumClass.cast(getConstructorAccessor(enumClass, additionalTypes)!!.newInstance(params))
//    }

    // 创建新的枚举实例
    inline fun <reified T : Any> makeEnum(
        enumType: Class<T>,
        name: String,
        ordinal: Int,
        additionalValues: Array<out Any>
    ): T {
        val unsafe = getUnsafe()
        val obj = unsafe.allocateInstance(enumType)

        // 使用Unsafe设置name、ordinal字段
        setEnumFieldValue(obj, "name", name)
        setEnumFieldValue(obj, "ordinal", ordinal)

        // 初始化自定义字段
        val enumFields =
            enumType.declaredFields.filterNot { it.isSynthetic || it.isEnumConstant || it.name == "Companion" }
        if (additionalValues.size < enumFields.size) {
            logger.warn("additionalValues size(${additionalValues.size}) less than enumField size(${enumFields.size})")
        } else {
            enumFields.forEachIndexed { index, field ->
                field.isAccessible = true
                field.set(obj, additionalValues[index])
            }
        }

        return obj as T
    }

    // 使用Unsafe设置枚举字段值
    fun setEnumFieldValue(enumInstance: Any, fieldName: String, value: Any) {
        try {
            val field = enumInstance.javaClass.superclass.getDeclaredField(fieldName)
            val unsafe = getUnsafe()
            val offset = unsafe.objectFieldOffset(field)
            when (value) {
                is Int -> unsafe.putInt(enumInstance, offset, value)
                is Long -> unsafe.putLong(enumInstance, offset, value)
                is Short -> unsafe.putShort(enumInstance, offset, value)
                is Byte -> unsafe.putByte(enumInstance, offset, value)
                is Float -> unsafe.putFloat(enumInstance, offset, value)
                is Double -> unsafe.putDouble(enumInstance, offset, value)
                is Boolean -> unsafe.putBoolean(enumInstance, offset, value)
                is Char -> unsafe.putChar(enumInstance, offset, value)
                else -> unsafe.putObject(enumInstance, offset, value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 从 JVM 获取 Unsafe 实例
    fun getUnsafe(): Unsafe {
        return try {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        } catch (e: Exception) {
            throw RuntimeException("Unsafe not found", e)
        }
    }

    val reflectionFactory: ReflectionFactory = ReflectionFactory.getReflectionFactory()
    val logger: Logger = LoggerFactory.getLogger(EnumUtil::class.java)
}
