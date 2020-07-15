package com.tencent.devops.gitci.utils

import com.tencent.devops.common.api.util.EmojiUtil
import org.junit.Assert
import org.junit.Test

internal class CommonUtilsTest {

    @Test
    fun getRepoNane() {
        Assert.assertEquals("royalhuang", CommonUtils.getRepoName("https://git.dev.code.oa.com/royalhuang/gitci-test.git"))
        Assert.assertEquals("royalhuang", CommonUtils.getRepoName("http://git.dev.code.oa.com/royalhuang/gitci-test.git"))
    }
}