package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.stream.common.exception.QualityRulesException
import com.tencent.devops.stream.pojo.v2.QualityElementInfo
import org.junit.jupiter.api.Test

internal class ModelStageTest {

    private val modelStage = ModelStage(mock(), mock(), mock())

    @Test
    fun getAtomCodeAndOther() {
        val (atomCode1, stepName1, mid1) = modelStage.getAtomCodeAndOther(
            "qualityTest.quality123.caseCnt_total >= 1.12",
            modelStage.operations
        )
        assert(
            atomCode1 == "qualityTest" && stepName1 == "quality123" && mid1 == "caseCnt_total >= 1.12"
        )

        val (atomCode2, stepName2, mid2) = modelStage.getAtomCodeAndOther(
            "qualityTest.quality*.caseCnt_total >= 1.12",
            modelStage.operations
        )
        assert(
            atomCode2 == "qualityTest" && stepName2 == "quality*" && mid2 == "caseCnt_total >= 1.12"
        )

        val (atomCode3, stepName3, mid3) = modelStage.getAtomCodeAndOther(
            "qualityTest.caseCnt_total >= 1.12",
            modelStage.operations
        )
        assert(
            atomCode3 == "qualityTest" && stepName3 == null && mid3 == "caseCnt_total >= 1.12"
        )

        var op = ""
        run breaking@{
            modelStage.operations.keys.forEach {
                if (mid3.contains(it)) {
                    op = it
                    return@breaking
                }
            }
        }

        assert(op == ">=")
    }

    @Test
    fun checkAndGetRealStepName() {

        val elementNames = mutableListOf(
            QualityElementInfo("test1", "test"),
            QualityElementInfo("test2", "test"),
            QualityElementInfo("tesa", "tesa")
        )

        var stepName = "test*"

        try {
            modelStage.checkAndGetRealStepName(stepName, elementNames)
        } catch (e: QualityRulesException) {
            assert(e.message!!.contains("there are multiple matches with"))
        }

        elementNames.removeAt(1)

        assert(modelStage.checkAndGetRealStepName(stepName, elementNames) == "test")

        stepName = "tes"
        try {
            modelStage.checkAndGetRealStepName(stepName, elementNames)
        } catch (e: QualityRulesException) {
            assert(e.message!!.contains("there none matches with"))
        }
    }
}
