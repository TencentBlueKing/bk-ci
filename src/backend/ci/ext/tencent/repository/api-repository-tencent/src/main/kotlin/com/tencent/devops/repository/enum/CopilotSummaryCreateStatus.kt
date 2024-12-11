package com.tencent.devops.repository.enum

/**
 * AI摘要生成状态
 */
enum class CopilotSummaryCreateStatus constructor(val value: Int) {
    SUCCESS(5),
    FAILURE(3),
    RUNNING(1)
}