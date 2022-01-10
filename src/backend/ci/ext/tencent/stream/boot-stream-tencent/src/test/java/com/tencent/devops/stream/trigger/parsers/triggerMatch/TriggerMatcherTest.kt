package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import org.junit.Assert
import org.junit.Test
import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.ci.v2.MrRule
import com.tencent.devops.common.ci.v2.PushRule
import com.tencent.devops.common.ci.v2.TagRule
import com.tencent.devops.stream.v2.service.StreamOauthService
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.stream.pojo.enums.StreamMrEventAction
import com.tencent.devops.stream.v2.service.StreamScmService

internal class TriggerMatcherTest {
    private val oauthService: StreamOauthService = StreamOauthService(mock(), mock(), mock())

    private val streamScmService: StreamScmService = StreamScmService(mock(), mock(), oauthService, mock())
    private val streamTimerService: StreamTimerService = StreamTimerService(
        mock(), mock(), mock()
    )

    private val triggerMatcher = TriggerMatcher(streamScmService, streamTimerService)

    @Test
    fun mrTest8() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    action = listOf(
                        StreamMrEventAction.REOPEN.value,
                        StreamMrEventAction.PUSH_UPDATE.value
                    ),
                    users = null,
                    usersIgnore = listOf("yongyiduan")
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "tangruotian",
            targetBranch = "master",
            changeSet = setOf("test.md"),
            mrAction = StreamMrEventAction.REOPEN.value
        )
        Assert.assertTrue(result)
    }

    @Test
    fun mrTest7() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    action = listOf(
                        StreamMrEventAction.REOPEN.value,
                        StreamMrEventAction.PUSH_UPDATE.value
                    ),
                    users = null,
                    usersIgnore = listOf("yongyiduan")
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("test.md"),
            mrAction = StreamMrEventAction.REOPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest6() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    action = listOf(
                        StreamMrEventAction.REOPEN.value,
                        StreamMrEventAction.PUSH_UPDATE.value
                    ),
                    users = listOf("tangruotian"),
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("test.md"),
            mrAction = StreamMrEventAction.REOPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest5() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    action = listOf(
                        StreamMrEventAction.REOPEN.value,
                        StreamMrEventAction.PUSH_UPDATE.value
                    ),
                    users = null,
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("test.md"),
            mrAction = StreamMrEventAction.OPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest4() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    action = null,
                    users = null,
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("/src/test.md"),
            mrAction = StreamMrEventAction.OPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest3() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master"),
                    paths = listOf("/src/*"),
                    pathsIgnore = null,
                    action = null,
                    users = null,
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("test.md"),
            mrAction = StreamMrEventAction.OPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest2() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release"),
                    targetBranches = listOf("master_1"),
                    paths = null,
                    pathsIgnore = null,
                    action = null,
                    users = null,
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("/src/test.md"),
            mrAction = StreamMrEventAction.OPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun mrTest1() {
        val result = triggerMatcher.isMrMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = null,
                mr = MrRule(
                    sourceBranchesIgnore = listOf("release_1"),
                    targetBranches = null,
                    paths = null,
                    pathsIgnore = null,
                    action = null,
                    users = null,
                    usersIgnore = null
                ),
                schedules = null
            ),
            sourceBranch = "release_1",
            userId = "yongyiduan",
            targetBranch = "master",
            changeSet = setOf("/src/test.md"),
            mrAction = StreamMrEventAction.OPEN.value
        )
        Assert.assertFalse(result)
    }

    @Test
    fun tagTest7() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = listOf("master"),
                    users = null,
                    usersIgnore = listOf("yongyiduan")
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_6666",
            userId = "ruotiantang",
            fromBranch = "master"
        )
        Assert.assertTrue(result)
    }

    @Test
    fun tagTest6() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = listOf("master"),
                    users = null,
                    usersIgnore = listOf("yongyiduan")
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_6666",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun tagTest5() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = listOf("master"),
                    users = listOf("ruotiantang"),
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_6666",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun tagTest4() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = listOf("master"),
                    users = null,
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_6666",
            userId = "yongyiduan",
            fromBranch = "release"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun tagTest3m() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = null,
                    users = null,
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_1025",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertTrue(result)
    }

    @Test
    fun tagTest3() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = listOf("release_1024"),
                    fromBranches = null,
                    users = null,
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_1024",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun tagTest2() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = null,
                    fromBranches = null,
                    users = null,
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "release_1024",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertTrue(result)
    }

    @Test
    fun tagTest1() {
        val result = triggerMatcher.isTagPushMatch(
            triggerOn = TriggerOn(
                push = null,
                tag = TagRule(
                    tags = listOf("release_*"),
                    tagsIgnore = null,
                    fromBranches = null,
                    users = null,
                    usersIgnore = null
                ),
                mr = null,
                schedules = null
            ),
            eventTag = "hello_world",
            userId = "yongyiduan",
            fromBranch = "master"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun pushTest6() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    users = null,
                    usersIgnore = listOf("yongyiduan")
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "release",
            changeSet = setOf("test.md"),
            userId = "yongyiduan"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun pushTest5() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    users = listOf("ruotiantang"),
                    usersIgnore = null
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "release",
            changeSet = setOf("test.md"),
            userId = "yongyiduan"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun pushTest4() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = listOf("/src/*"),
                    users = null,
                    usersIgnore = null
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "release",
            changeSet = setOf("/src/test.md"),
            userId = "yongyiduan"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun pushTest3() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = listOf("master"),
                    paths = listOf("*"),
                    pathsIgnore = null,
                    users = null,
                    usersIgnore = null
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "release",
            changeSet = setOf("/src/test.md"),
            userId = "yongyiduan"
        )
        Assert.assertTrue(result)
    }

    @Test
    fun pushTest2() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = listOf("master"),
                    paths = null,
                    pathsIgnore = null,
                    users = null,
                    usersIgnore = null
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "master",
            changeSet = null,
            userId = "yongyiduan"
        )
        Assert.assertFalse(result)
    }

    @Test
    fun pushTest1() {
        val result = triggerMatcher.isPushMatch(
            triggerOn = TriggerOn(
                push = PushRule(
                    branches = listOf("*"),
                    branchesIgnore = null,
                    paths = null,
                    pathsIgnore = null,
                    users = null,
                    usersIgnore = null
                ),
                tag = null,
                mr = null,
                schedules = null
            ),
            eventBranch = "master",
            changeSet = null,
            userId = "yongyiduan"
        )
        Assert.assertTrue(result)
    }
}
