package com.tencent.devops.stream.trigger.actions.tgit

import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TGit Mr Action 相关测试")
internal class TGitMrActionGitTest {

    private val action = TGitMrActionGit(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())

    /**
     * 校验
     * 源有，目标无，变更有
     * 源有，目标无，变更无
     * 源有，目标有，变更有
     * 源有，目标有，变更无
     */
    @DisplayName("对比源分支相比较目标分支")
    @Test
    fun checkMrYamlPathList() {
        val sources = setOf(Pair("1", "1"), Pair("2", "2"), Pair("3", "3"), Pair("4", "4"))
        val target = setOf(Pair("1", "1"), Pair("4", "4"))
        val changeSet = setOf("1")
        val result = action.checkMrYamlPathList(sources, target, changeSet, "source", "master")
        val compare = listOf(
            YamlPathListEntry("1", CheckType.NEED_CHECK, "source", "1"),
            YamlPathListEntry("2", CheckType.NO_TRIGGER, "source", "2"),
            YamlPathListEntry("3", CheckType.NO_TRIGGER, "source", "3"),
            YamlPathListEntry("4", CheckType.NO_NEED_CHECK, "master", "4")
        )

        Assertions.assertEquals(compare, result)
    }

    /**
     * 校验
     * 源无，目标有，变更有
     * 源无，目标有，变更无
     * 源无，目标无，变更有
     * 源无，目标无，变更无
     */
    @DisplayName("对比目标分支相比较源分支")
    @Test
    fun checkMrYamlPathList2() {
        val sources = setOf(Pair("3", "3"))
        val target = setOf(Pair("1", "1"), Pair("2", "2"))
        val changeSet = setOf("1", "5")
        val result = action.checkMrYamlPathList(sources, target, changeSet, "source", "master")
        val compare = listOf(
            YamlPathListEntry("3", CheckType.NO_TRIGGER, "source", "3"),
            YamlPathListEntry("2", CheckType.NO_NEED_CHECK, "master", "2")
        )

        Assertions.assertEquals(compare, result)
    }
}
