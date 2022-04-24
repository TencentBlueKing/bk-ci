package com.tencent.devops.stream.trigger.actions.tgit

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.stream.trigger.pojo.CheckType
import org.junit.jupiter.api.Test

internal class TGitMrActionGitTest {

    private val action = TGitMrActionGit(mock(), mock(), mock(), mock(), mock())

    @Test
    /**
     * 校验
     * 源有，目标无，变更有
     * 源有，目标无，变更无
     * 源有，目标有，变更有
     * 源有，目标有，变更无
     */
    @org.junit.Test
    fun checkMrYamlPathList() {
        val sources = setOf("1", "2", "3", "4")
        val target = setOf("1", "4")
        val changeSet = setOf("1")
        val result = action.checkMrYamlPathList(sources, target, changeSet)
        val compare = mapOf(
            "1" to CheckType.NEED_CHECK,
            "2" to CheckType.NO_TRIGGER,
            "3" to CheckType.NO_TRIGGER,
            "4" to CheckType.NO_NEED_CHECK
        )
        assert(result == compare)
    }

    /**
     * 校验
     * 源无，目标有，变更有
     * 源无，目标有，变更无
     * 源无，目标无，变更有
     * 源无，目标无，变更无
     */
    @org.junit.Test
    fun checkMrYamlPathList2() {
        val sources = setOf("3")
        val target = setOf("1", "2")
        val changeSet = setOf("1", "5")
        val result = action.checkMrYamlPathList(sources, target, changeSet)
        val compare = mapOf("2" to CheckType.NO_NEED_CHECK, "3" to CheckType.NO_TRIGGER)
        assert(result == compare)
    }
}
