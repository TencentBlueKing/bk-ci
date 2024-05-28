/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import org.apache.commons.lang3.reflect.MethodUtils
import sun.reflect.ReflectionFactory
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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

            // 构建新枚举值
            val newValue: T = makeEnum(
                enumClass = enumType,
                value = enumName,
                ordinal = ordinal,
                additionalTypes = additionalTypes.toTypedArray(),
                additionalValues = additionalValues
            )

            if (ordinal < previousValues.size) {
                values[ordinal] = newValue
            } else {
                values.add(newValue)
            }

            setFailSafeFieldValue(field = valuesField, target = null, value = values.toTypedArray())

            cleanEnumCache(enumType)
        } catch (e: Exception) {
            println(e)
            e.printStackTrace()
            throw RuntimeException(e.message, e)
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun setFailSafeFieldValue(field: Field, target: Any?, value: Any?) {
        field.isAccessible = true
        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        var modifiers: Int = modifiersField.getInt(field)
        modifiers = modifiers and Modifier.FINAL.inv()
        modifiersField.setInt(field, modifiers)

        val fieldAccessor = MethodUtils.invokeMethod(field, true, "acquireFieldAccessor", false)
        MethodUtils.invokeMethod(
            fieldAccessor,
            true,
            "set",
            arrayOf(target, value),
            arrayOf<Class<*>>(Object::class.java, Object::class.java)
        )
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    inline fun <reified T : Any> blankField(enumClass: Class<T>, fieldName: String) {
        for (field in Class::class.java.declaredFields) {
            if (field.name.contains(fieldName)) {
                setFailSafeFieldValue(field, enumClass, null)
                break
            }
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    inline fun <reified T : Any> cleanEnumCache(enumClass: Class<T>) {
        blankField(enumClass, "enumConstantDirectory") // OracleJDK & OpenJDK
        blankField(enumClass, "enumConstants") // IBM JDK
    }

    @Throws(NoSuchMethodException::class)
    inline fun <reified T : Any> getConstructor(
        enumClass: Class<T>,
        additionalParameterTypes: Array<Class<out Any>>
    ): Constructor<out Any>? {
        val parameterTypes = arrayOfNulls<Class<*>?>(additionalParameterTypes.size + 2)
        parameterTypes[0] = String::class.java // enum class first field: field name
        parameterTypes[1] = Int::class.javaPrimitiveType // enum class second field: ordinal
        System.arraycopy(additionalParameterTypes, 0, parameterTypes, 2, additionalParameterTypes.size)
        enumClass.declaredConstructors.forEach { constructor ->
            if (compareParameterType(constructor.parameterTypes, parameterTypes)) {
                try {
                    constructor.isAccessible = true
                    return constructor
                } catch (ignored: IllegalArgumentException) {
                    // skip illegal argument try next one
                }
            }
        }

        val constructor = enumClass.getDeclaredConstructor(*parameterTypes)
        constructor.isAccessible = true
        return constructor
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

    @Throws(Exception::class)
    inline fun <reified T : Any> makeEnum(
        enumClass: Class<T>,
        value: String,
        ordinal: Int,
        additionalTypes: Array<Class<out Any>>,
        additionalValues: Array<out Any>
    ): T {
        val params = arrayOfNulls<Any>(additionalValues.size + 2)
        params[0] = value
        params[1] = Integer.valueOf(ordinal)
        System.arraycopy(additionalValues, 0, params, 2, additionalValues.size)
        val constructor = getConstructor(enumClass, additionalTypes)
        val constructorAccessor = MethodUtils.invokeMethod(constructor, true, "acquireConstructorAccessor")
        val instance = MethodUtils.invokeMethod(constructorAccessor, true, "newInstance", params)
        return enumClass.cast(instance)
    }

    val reflectionFactory: ReflectionFactory = ReflectionFactory.getReflectionFactory()
}
