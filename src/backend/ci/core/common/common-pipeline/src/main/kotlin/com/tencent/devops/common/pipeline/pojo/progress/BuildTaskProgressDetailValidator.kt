package com.tencent.devops.common.pipeline.pojo.progress

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

object BuildTaskProgressDetailValidator {
    const val MAX_PAYLOAD_BYTES = 32 * 1024

    private const val MAX_TITLE_LENGTH = 128
    private const val MAX_SUMMARY_LENGTH = 32
    private const val MAX_SUBTASK_ITEMS = 100
    private const val MAX_TIMELINE_ITEMS = 50
    private const val PROGRESS_SCALE = 4

    fun isPayloadTooLarge(payload: String): Boolean {
        return payload.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES
    }

    fun normalizeProgress(value: Double): Double {
        require(value.isFinite()) { "progress value must be finite" }
        require(value in 0.0..1.0) { "progress value must be between 0 and 1" }
        return BigDecimal.valueOf(value).setScale(PROGRESS_SCALE, RoundingMode.HALF_UP).toDouble()
    }

    fun normalize(detail: BuildTaskProgressDetail): BuildTaskProgressDetail {
        val progress = detail.progress
        validateNullableText(progress.title, MAX_TITLE_LENGTH, "progress.title")
        validateNullableText(progress.summary, MAX_SUMMARY_LENGTH, "progress.summary")

        return detail.copy(
            progress = progress.copy(value = normalizeProgress(progress.value)),
            subtasks = detail.subtasks?.let(::normalizeSubtasks),
            timeline = detail.timeline?.let(::normalizeTimeline)
        )
    }

    private fun normalizeSubtasks(subtasks: BuildTaskSubtaskProgressGroup): BuildTaskSubtaskProgressGroup {
        validateNullableText(subtasks.title, MAX_TITLE_LENGTH, "subtasks.title")
        validateNullableText(subtasks.summary, MAX_SUMMARY_LENGTH, "subtasks.summary")
        require((subtasks.items?.size ?: 0) <= MAX_SUBTASK_ITEMS) {
            "subtasks.items size must be less than or equal to $MAX_SUBTASK_ITEMS"
        }
        return subtasks.copy(
            items = subtasks.items?.mapIndexed { index, item ->
                validateRequiredText(item.name, MAX_TITLE_LENGTH, "subtasks.items[$index].name")
                item.copy(progress = normalizeProgress(item.progress))
            }
        )
    }

    private fun normalizeTimeline(timeline: BuildTaskProgressTimeline): BuildTaskProgressTimeline {
        validateNullableText(timeline.title, MAX_TITLE_LENGTH, "timeline.title")
        require((timeline.items?.size ?: 0) <= MAX_TIMELINE_ITEMS) {
            "timeline.items size must be less than or equal to $MAX_TIMELINE_ITEMS"
        }
        return timeline.copy(
            items = timeline.items?.mapIndexed { index, item ->
                validateRequiredText(item.name, MAX_TITLE_LENGTH, "timeline.items[$index].name")
                validateInstant(item.startTime, "timeline.items[$index].startTime")
                require(item.duration == null || item.duration >= 0) {
                    "timeline.items[$index].duration must be null or non-negative"
                }
                item
            }
        )
    }

    private fun validateNullableText(value: String?, maxLength: Int, fieldName: String) {
        if (value != null) {
            validateText(value, maxLength, fieldName, allowBlank = true)
        }
    }

    private fun validateRequiredText(value: String, maxLength: Int, fieldName: String) {
        validateText(value, maxLength, fieldName, allowBlank = false)
    }

    private fun validateText(value: String, maxLength: Int, fieldName: String, allowBlank: Boolean) {
        require(allowBlank || value.isNotBlank()) { "$fieldName must not be blank" }
        require(value.length <= maxLength) { "$fieldName length must be less than or equal to $maxLength" }
    }

    private fun validateInstant(value: String, fieldName: String) {
        runCatching { Instant.parse(value) }
            .getOrElse { throw IllegalArgumentException("$fieldName must be an ISO 8601 UTC instant", it) }
    }
}
