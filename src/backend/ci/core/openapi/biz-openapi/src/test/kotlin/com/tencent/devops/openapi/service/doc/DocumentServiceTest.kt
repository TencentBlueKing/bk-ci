package com.tencent.devops.openapi.service.doc

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.tencent.devops.common.web.JerseyConfig
import com.tencent.devops.openapi.pojo.SwaggerDocParameterInfo
import io.swagger.annotations.ApiModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaType

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [JerseyConfig::class, DocumentService::class])
class DocumentServiceTest @Autowired constructor(
    private val document: DocumentService
) {
    @Test
    fun docInit() {
        try {
            val config = ConfigurationBuilder()
            config.addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
            config.setExpandSuperTypes(true)
            config.setScanners(Scanners.TypesAnnotated)
            val reflections = Reflections(config)

            val doc = document.docInit(
                checkMetaData = true,
                checkMDData = true,
                polymorphism = getAllSubType(reflections),
                outputPath = "build/swaggerDoc/",
                parametersInfo = getAllApiModelInfo(reflections)
            )
            println("${doc.size}|${doc.keys}")
        } catch (e: Throwable) {
            // 抛错时不影响Test流程
            println("docInit error")
            e.printStackTrace()
        }
    }

    /**
     *  获取所有多态类的实现信息
     */
    fun getAllSubType(reflections: Reflections): Map<String, Map<String, String>> {
        val subTypesClazz = reflections.getTypesAnnotatedWith(JsonSubTypes::class.java)
        val res = mutableMapOf<String, Map<String, String>>()
        subTypesClazz.forEach {
            val infoMap = mutableMapOf<String, String>()
            val subTypes = it.getAnnotation(JsonSubTypes::class.java).value
//            val typeInfo = it.getAnnotation(JsonTypeInfo::class.java).property
            val name = it.getAnnotation(ApiModel::class.java)?.value ?: it.name.split(".").last()
            subTypes.forEach { child ->
                val childName = child.value.java.getAnnotation(ApiModel::class.java)?.value
                    ?: child.value.java.name.split(".").last()
                infoMap[childName] = child.name
            }
            res[name] = infoMap
        }
        return res
    }

    fun getAllApiModelInfo(reflections: Reflections): Map<String, Map<String, SwaggerDocParameterInfo>> {
        val clazz = reflections.getTypesAnnotatedWith(ApiModel::class.java).toList()
        val res = mutableMapOf<String, Map<String, SwaggerDocParameterInfo>>()
        for (i in 0 until clazz.size) {
            val it = clazz.getOrNull(i) ?: continue

            println("$i${it.name}")
            try {
                val name = it.getAnnotation(ApiModel::class.java).value
                res[name] = getDataClassParameterDefault(it)
            } catch (e: Throwable) {
//                println(it.name)
//                println(e)
            }
        }
        return res
    }

    /**
     * 例子:
     * ```java
     *  getDataClassParameterDefault(Class.forName("com.tencent.devops.openapi.pojo.SwaggerDocResponse"))
     * ```
     *  @param clazz 目标类
     *  @return 带默认值的map
     */
    @Suppress("ComplexMethod")
    fun getDataClassParameterDefault(clazz: Class<*>): Map<String, SwaggerDocParameterInfo> {
        val kClazz = clazz.kotlin
        if (!kClazz.isData) return emptyMap()
        val constructor = kClazz.constructors.maxByOrNull { it.parameters.size }!!
        val parameters = constructor.parameters
        val syntheticInit = clazz.declaredConstructors.find { it.modifiers == 4097 }
        val argumentsSize = syntheticInit?.parameterTypes?.size ?: parameters.size
        val arguments = arrayOfNulls<Any>(argumentsSize)
        var index = 0
        var offset = 0
        val nullable = mutableMapOf<String, Boolean>()
        parameters.forEach {
            if (it.isOptional) {
                offset += 1 shl index
            }
            nullable[it.name ?: ""] = it.type.isMarkedNullable
            arguments[index++] = makeStandardArgument(it.type, constructor)
        }
        for (i in index until argumentsSize - 2) {
            arguments[i] = 0
        }
        if (syntheticInit != null) {
            arguments[argumentsSize - 2] = offset
            arguments[argumentsSize - 1] = null as DefaultConstructorMarker?
        }
        val mock = (syntheticInit ?: constructor.javaConstructor)!!.newInstance()
//        val mock = try {
        val res = mutableMapOf<String, SwaggerDocParameterInfo>()
        kClazz.memberProperties.forEach {
            // 编译后，属性默认是private,需要设置isAccessible  才可以读取到值
            it.isAccessible = true
            res[it.name] = SwaggerDocParameterInfo(
                markedNullable = nullable[it.name] ?: false,
                defaultValue = checkDefaultValue(it.call(mock).toString())
            )
        }
        return res
    }

    @Suppress("ComplexCondition")
    private fun checkDefaultValue(v: String): String? {
        if (v.startsWith("Mock") || v.isBlank() || v == "[]" || v == "{=}" || v == "{}") return null
        return v
    }

    @Suppress("ComplexMethod")
    private fun makeStandardArgument(type: KType, debug: KFunction<*>): Any? {
        if (type.isMarkedNullable) return null
        return when (type.classifier) {
            Boolean::class -> false
            Byte::class -> 0.toByte()
            Short::class -> 0.toShort()
            Char::class -> 0.toChar()
            Int::class -> 0
            Long::class -> 0L
            Float::class -> 0f
            Double::class -> 0.0
            String::class -> ""
            Enum::class -> {
                null
            }

            Set::class -> {
                type.arguments.firstOrNull()?.let { setOf(makeStandardArgument(it.type!!, debug)) } ?: ""
            }

            List::class -> {
                type.arguments.firstOrNull()?.let { listOf(makeStandardArgument(it.type!!, debug)) } ?: ""
            }

            ArrayList::class -> {
                type.arguments.firstOrNull()?.let { arrayListOf(makeStandardArgument(it.type!!, debug)) } ?: ""
            }

            Array::class -> {
                type.arguments.firstOrNull()?.let { arrayOf(makeStandardArgument(it.type!!, debug)) } ?: ""
            }

            Map::class -> {
                mapOf(
                    makeStandardArgument(
                        type.arguments[0].type!!,
                        debug
                    ) to makeStandardArgument(type.arguments[1].type!!, debug)
                )
            }

            else -> {
                if (type.javaType is Class<*>) {
                    (type.javaType as Class<*>).newInstance()
                } else {
                    null
                }
            }
        }
    }
}
