package com.tencent.devops.repository.enums

/**
 * AI摘要生成状态
 */
enum class CopilotSummaryCreateStatus constructor(val value: Int) {
    SUCCESS(5),
    FAILURE(3),
    RUNNING(1);

    companion object {
        fun isFinal(status: Int): Boolean {
            return status == SUCCESS.value || status == FAILURE.value
        }
    }
}