package com.tencent.devops.gitci.utils

import org.junit.Assert
import org.junit.Test

internal class GitCommonUtilsTest {

    @Test
    fun getRepoNane() {
        Assert.assertEquals("royalhuang", GitCommonUtils.getRepoOwner("https://git.dev.code.oa.com/royalhuang/gitci-test.git"))
        Assert.assertEquals("royalhuang", GitCommonUtils.getRepoOwner("http://git.dev.code.oa.com/royalhuang/gitci-test.git"))
    }
}
