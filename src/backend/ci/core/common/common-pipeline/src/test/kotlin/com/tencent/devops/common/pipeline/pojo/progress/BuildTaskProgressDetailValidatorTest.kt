package com.tencent.devops.common.pipeline.pojo.progress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildTaskProgressDetailValidatorTest {

    @Test
    fun normalizeProgress() {
        Assertions.assertEquals(0.3334, BuildTaskProgressDetailValidator.normalizeProgress(0.33335))
        Assertions.assertEquals(0.0, BuildTaskProgressDetailValidator.normalizeProgress(0.0))
        Assertions.assertEquals(1.0, BuildTaskProgressDetailValidator.normalizeProgress(1.0))
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            BuildTaskProgressDetailValidator.normalizeProgress(1.1)
        }
    }

    @Test
    fun normalizeDetail() {
        val detail = BuildTaskProgressDetail(
            progress = BuildTaskProgressSummary(value = 0.12345),
            subtasks = BuildTaskSubtaskProgressGroup(
                items = listOf(
                    BuildTaskSubtaskProgressItem(
                        name = "download",
                        progress = 0.55555,
                        status = BuildTaskProgressStatus.RUNNING
                    )
                )
            ),
            timeline = BuildTaskProgressTimeline(
                items = listOf(
                    BuildTaskProgressTimelineItem(
                        name = "prepare",
                        startTime = "2026-05-27T07:00:00Z",
                        duration = null
                    )
                )
            )
        )

        val normalized = BuildTaskProgressDetailValidator.normalize(detail)

        Assertions.assertEquals(0.1235, normalized.progress.value)
        Assertions.assertEquals(0.5556, normalized.subtasks?.items?.first()?.progress)
    }

    @Test
    fun rejectInvalidDetail() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            BuildTaskProgressDetailValidator.normalize(
                BuildTaskProgressDetail(
                    progress = BuildTaskProgressSummary(value = 0.5),
                    subtasks = BuildTaskSubtaskProgressGroup(
                        items = listOf(
                            BuildTaskSubtaskProgressItem(
                                name = "",
                                progress = 0.1,
                                status = BuildTaskProgressStatus.PENDING
                            )
                        )
                    )
                )
            )
        }
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            BuildTaskProgressDetailValidator.normalize(
                BuildTaskProgressDetail(
                    progress = BuildTaskProgressSummary(value = 0.5),
                    timeline = BuildTaskProgressTimeline(
                        items = listOf(
                            BuildTaskProgressTimelineItem(
                                name = "prepare",
                                startTime = "2026-05-27 07:00:00",
                                duration = 1
                            )
                        )
                    )
                )
            )
        }
    }
}
