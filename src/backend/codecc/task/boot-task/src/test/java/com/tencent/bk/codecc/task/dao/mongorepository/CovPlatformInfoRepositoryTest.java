package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.TaskBootApplication;
import com.tencent.bk.codecc.task.model.CovPlatformInfoEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = TaskBootApplication.class)
public class CovPlatformInfoRepositoryTest
{
    @Autowired
    private CovPlatformInfoRepository covPlatformInfoRepository;

//    @Test
    public void testAddCovPlatformInfo()
    {
        String[] ips = {"9.134.77.169",
                "9.134.78.34",
                "9.134.79.253",
                "9.134.69.208",
                "9.134.79.12",
                "9.134.79.128",
                "9.134.79.125",
                "9.134.75.231"};

        long currTime = System.currentTimeMillis();
        for (String ip: ips)
        {
            CovPlatformInfoEntity covPlatformInfoEntity = new CovPlatformInfoEntity();
            covPlatformInfoEntity.setIp(ip);
            covPlatformInfoEntity.setPort("8080");
            covPlatformInfoEntity.setPasswd("coverity1");
            covPlatformInfoEntity.setUserName("admin");
            covPlatformInfoEntity.setOwner("jimxzcai");
            covPlatformInfoEntity.setStatus(ComConstants.Status.ENABLE.value());
            covPlatformInfoEntity.setCreatedBy("austinshen");
            covPlatformInfoEntity.setCreatedDate(currTime);
            covPlatformInfoRepository.save(covPlatformInfoEntity);
        }

    }

}