package com.tencent.bk.devops.atom.utils.http;

import org.junit.Assert;
import org.junit.Test;

public class SdkUtilsTest {

    @Test
    public void getDataDir() {
        System.out.println(SdkUtils.getDataDir());
    }

    @Test
    public void trimProtocol() {
        String host = "www.tencent.com";
        Assert.assertEquals(host, SdkUtils.trimProtocol("http://www.tencent.com/"));
        Assert.assertEquals(host, SdkUtils.trimProtocol("http://www.tencent.com/dodi"));
        Assert.assertEquals(host, SdkUtils.trimProtocol("https://www.tencent.com/dodi"));
        Assert.assertEquals(host, SdkUtils.trimProtocol("https://www.tencent.com"));
        host = "www.ten-cent.com";
        Assert.assertEquals(host, SdkUtils.trimProtocol("https://www.ten-cent.com"));
    }

    @Test
    public void hasProtocol() {
        Assert.assertTrue(SdkUtils.hasProtocol("http://www.tencent.com/"));
        Assert.assertTrue(SdkUtils.hasProtocol("http://www.tencent.com/dodi"));
        Assert.assertTrue(SdkUtils.hasProtocol("https://www.tencent.com/dodi"));
        Assert.assertTrue(SdkUtils.hasProtocol("https://www.tencent.com"));
        Assert.assertTrue(SdkUtils.hasProtocol("https://www.ten-cent.com"));
    }
}