package com.tencent.devops.common.webhook.service.code.filter

import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PushKindFilterTest {

    private val response = WebhookFilterResponse()

    @Test
    fun includeBranches() {
        var pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = emptyList(),
            checkCreateAndUpdate = null
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value, TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = null
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value),
            checkCreateAndUpdate = null
        )

        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = null
        )

        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = emptyList(),
            checkCreateAndUpdate = true
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value, TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = true
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value),
            checkCreateAndUpdate = true
        )

        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = true
        )

        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = emptyList(),
            checkCreateAndUpdate = false
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value, TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = false
        )
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            actionList = listOf(TGitPushActionType.PUSH_FILE.value),
            checkCreateAndUpdate = false
        )

        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedee",
            actionList = listOf(TGitPushActionType.NEW_BRANCH.value),
            checkCreateAndUpdate = false
        )

        Assertions.assertTrue(pushKindFilter.doFilter(response))
    }
}
