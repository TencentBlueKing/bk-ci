package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.expression.EvalExpress
import com.tencent.devops.common.pipeline.pojo.VarRefDetail
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ModelVarRefValidatorTest {

    private fun ref(varName: String, isDoubleBrace: Boolean) = VarRefDetail(
        projectId = "p1",
        varName = varName,
        resourceId = "pipe1",
        resourceType = "PIPELINE",
        positionPath = "model.stages[1].containers[0].matrixControlOption.strategyStr",
        isDoubleBrace = isDoubleBrace
    )

    @Test
    fun doubleBrace_directContextPrefix_isValid() {
        Assertions.assertTrue(ModelVarRefValidator.isValidRef(ref("variables.parameters", true)))
        Assertions.assertTrue(ModelVarRefValidator.isValidRef(ref("matrix.urls", true)))
    }

    @Test
    fun doubleBrace_fromJsonForMatrixStrategy_isValid() {
        Assertions.assertTrue(
            ModelVarRefValidator.isValidRef(ref("fromJSON(variables.parameters)", true))
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "contains(variables.tag, 'release')",
            "join(variables.urls, ',')",
            "startsWith(variables.branch, 'feature/')",
            "endsWith(variables.name, '-prod')",
            "toJSON(variables.payload)",
            "format('hello {0}', variables.name)",
            "strToTime(variables.deadline)",
            "fromJSON(variables.parameters).urls",
            "variables.a == 'b'",
            "(contains(variables.env, 'prod') && variables.flag == true)"
        ]
    )
    fun doubleBrace_expressionFunctions_isValid(expression: String) {
        Assertions.assertTrue(ModelVarRefValidator.isValidRef(ref(expression, true)))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "parameters",
            "fromJSON(parameters)",
            "depends.job1.outputs.matrix",
            "fromJSON(depends.job1.outputs.matrix)",
            "fromJSONasd(variables.parameters)",
            "unknownFunc(variables.x)"
        ]
    )
    fun doubleBrace_invalidExpressions_areRejected(expression: String) {
        Assertions.assertFalse(ModelVarRefValidator.isValidRef(ref(expression, true)))
    }

    @Test
    fun singleBrace_mustNotUseContextPrefix() {
        Assertions.assertFalse(ModelVarRefValidator.isValidRef(ref("variables.foo", false)))
        Assertions.assertTrue(ModelVarRefValidator.isValidRef(ref("fromJSON(parameters)", false)))
        Assertions.assertTrue(ModelVarRefValidator.isValidRef(ref("parameters", false)))
    }

    @Test
    fun constrainedExpressionValidator_alignsWithContextPrefix() {
        val prefixes = EvalExpress.contextPrefix
        Assertions.assertTrue(
            ConstrainedExpressionValidator.isValidDoubleBraceExpression("fromJSON(variables.parameters)", prefixes)
        )
        Assertions.assertFalse(
            ConstrainedExpressionValidator.isValidDoubleBraceExpression("fromJSON(parameters)", prefixes)
        )
    }
}
