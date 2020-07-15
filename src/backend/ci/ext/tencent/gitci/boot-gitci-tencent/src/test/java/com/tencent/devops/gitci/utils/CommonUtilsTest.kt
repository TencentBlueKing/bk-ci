package com.tencent.devops.gitci.utils

import org.junit.Assert
import org.junit.Test

internal class CommonUtilsTest {

    @Test
    fun getRepoNane() {
        Assert.assertEquals("royalhuang", CommonUtils.getRepoOwner("https://git.dev.code.oa.com/royalhuang/gitci-test.git"))
        Assert.assertEquals("royalhuang", CommonUtils.getRepoOwner("http://git.dev.code.oa.com/royalhuang/gitci-test.git"))
    }
}