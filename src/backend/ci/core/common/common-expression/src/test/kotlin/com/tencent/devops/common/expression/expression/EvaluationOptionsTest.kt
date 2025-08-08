package com.tencent.devops.common.expression.expression

import com.tencent.devops.common.expression.ContextNotFoundException
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.ArrayContextData
import com.tencent.devops.common.expression.context.ContextValueNode
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory

@Suppress("ComplexMethod", "LongMethod", "MaxLineLength")
@DisplayName("测试EvaluationOptions配置的不同选项")
class EvaluationOptionsTest {
    @DisplayName("exceptionInsteadOfNull相关场景测试")
    @Nested
    inner class ExceptionTest {
        @DisplayName("配置了exceptionInsteadOfNull")
        @ParameterizedTest
        @ValueSource(
            strings = [
                "string => string",
                "obj.a.obj_obj1[2] == 1 => obj.a",
                "arr[0].arr_obj_1_n2 => arr.0.arr_obj_1_n2",
                "obj.obj_obj1.obj_obj1_n1.a => obj.obj_obj1.obj_obj1_n1.a"
            ]
        )
        fun exceptionInsteadOfNull(group: String) {
            val (exp, exArg) = group.split(" => ")
            val options = EvaluationOptions(true)
            assertThrows<ContextNotFoundException> {
                ExpressionParser.createTree(exp, null, nameValue, null)!!
                    .evaluate(TestTraceWriter(), ev, options, null)
            }
            println(options.contextNotNull.exceptionTraceMsg)
            Assertions.assertEquals(
                ContextNotFoundException(exArg).message,
                options.contextNotNull.exceptionTraceMsg?.joinToString(".")
            )
        }

        @DisplayName("不配置exceptionInsteadOfNull")
        @ParameterizedTest
        @ValueSource(
            strings = [
                "string == null",
                "obj.a.obj_obj1[2] == null",
                "arr[0].arr_obj_1_n2 == null",
                "obj.obj_obj1.obj_obj1_n1.a == null"
            ]
        )
        fun noExceptionInsteadOfNull(exp: String) {
            val options = EvaluationOptions(false)
            val result = ExpressionParser.createTree(exp, null, nameValue, null)!!
                .evaluate(TestTraceWriter(), ev, options, null)
            Assertions.assertTrue(result.equalsTrue)
        }

        private val nameValue = mutableListOf<NamedValueInfo>().apply {
            add(NamedValueInfo("string", ContextValueNode()))
            add(NamedValueInfo("obj", ContextValueNode()))
            add(NamedValueInfo("arr", ContextValueNode()))
        }
        private val ev = ExecutionContext(DictionaryContextData().apply {
            add("obj", DictionaryContextData().apply {
                add("obj_arr1", ArrayContextData().apply {
                    add(null)
                })
                add("obj_obj1", DictionaryContextData().apply {
                    add("obj_obj1_n1", DictionaryContextData().apply {
                    })
                })
            })
            add("arr", ArrayContextData().apply {
                add(DictionaryContextData().apply {
                    add("arr_obj_1_n1", StringContextData("arr_obj_1_n1_v"))
                })
            })
        })
    }
}

class TestTraceWriter : ITraceWriter {
    override fun info(message: String?) {
        logger.info(message ?: return)
    }

    override fun verbose(message: String?) {
        logger.debug(message ?: return)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TestTraceWriter::class.java)
    }
}