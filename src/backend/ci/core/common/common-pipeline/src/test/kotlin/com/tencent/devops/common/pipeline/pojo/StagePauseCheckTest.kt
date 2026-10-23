package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.dialect.ClassicPipelineDialect
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StagePauseCheckTest {

    @Test
    fun parseReviewParams() {
        val check = StagePauseCheck(
            manualTrigger = true,
            reviewParams = mutableListOf(
                ManualReviewParam(key = "p1", value = "111"),
                ManualReviewParam(key = "p2", value = "222")
            )
        )
        val originKeys = check.reviewParams?.map { it.key }?.toList()
        val params = mutableListOf(
            ManualReviewParam(key = "p1", value = "123"),
            ManualReviewParam(key = "p2", value = "222")
        )
        Assertions.assertEquals(
            mutableListOf(ManualReviewParam(key = "p1", value = "123")),
            check.parseReviewParams(params)
        )
        Assertions.assertEquals(
            check.reviewParams?.map { it.key }?.toList(),
            originKeys
        )
    }

    @Test
    fun splitAndSanitizeIds_shouldTrimWhitespaceAndNewlines() {
        Assertions.assertEquals(
            listOf("qqxxliu", "user2"),
            StagePauseCheck.splitAndSanitizeIds("\nqqxxliu, user2\r\n, ,\t")
        )
        Assertions.assertEquals(
            listOf("qqxxliu"),
            StagePauseCheck.sanitizeIds(listOf("\nqqxxliu", "  ", "\r\n"))
        )
    }

    @Test
    fun parseReviewVariables_shouldSanitizeReviewersFromVariables() {
        val check = StagePauseCheck(
            manualTrigger = true,
            reviewGroups = mutableListOf(
                StageReviewGroup(
                    reviewers = listOf("\${REVIEWERS}")
                )
            ),
            notifyGroup = mutableListOf("  chat1\n", "\n")
        )
        check.parseReviewVariables(
            variables = mapOf("REVIEWERS" to "\nqqxxliu, user2\r\n"),
            dialect = ClassicPipelineDialect()
        )
        Assertions.assertEquals(listOf("qqxxliu", "user2"), check.reviewGroups!![0].reviewers)
        Assertions.assertEquals(listOf("chat1"), check.notifyGroup)
    }
}
