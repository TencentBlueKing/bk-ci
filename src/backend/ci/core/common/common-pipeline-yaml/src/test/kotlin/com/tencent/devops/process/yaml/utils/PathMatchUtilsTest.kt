package com.tencent.devops.process.yaml.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PathMatchUtilsTest {

    @Test
    fun isIncludePathMatch() {
        var pathList = listOf("a/*")
        var fileChangeSet = setOf("a/b/c")
        PathMatchUtils.isIncludePathMatch(pathList, fileChangeSet).let {
            Assertions.assertEquals(it, true)
        }

        pathList = listOf("a/*", "*")
        fileChangeSet = setOf("a/b/c", "a")
        PathMatchUtils.isIncludePathMatch(pathList, fileChangeSet).let {
            Assertions.assertEquals(it, true)
        }

        pathList = listOf("a/**")
        fileChangeSet = setOf("a/b/c")
        PathMatchUtils.isIncludePathMatch(pathList, fileChangeSet).let {
            Assertions.assertEquals(it, true)
        }
// blob表达式暂不支持
//        pathList = listOf("**.{java,class}")
//        fileChangeSet = setOf("a/a.bat")
//        PathMatchUtils.isIncludePathMatch(pathList, fileChangeSet).let {
//            Assertions.assertEquals(it, false)
//        }
//
//        pathList = listOf("**.{java,class}")
//        fileChangeSet = setOf("a/a.class")
//        PathMatchUtils.isIncludePathMatch(pathList, fileChangeSet).let {
//            Assertions.assertEquals(it, true)
//        }
    }
}
