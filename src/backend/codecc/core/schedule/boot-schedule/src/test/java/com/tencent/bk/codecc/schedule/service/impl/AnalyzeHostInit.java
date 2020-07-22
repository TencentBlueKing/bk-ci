package com.tencent.bk.codecc.schedule.service.impl;

import com.tencent.bk.codecc.schedule.ScheduleBootApplication;
import com.tencent.bk.codecc.schedule.dao.mongorepository.AnalyzeHostRepository;
import com.tencent.bk.codecc.schedule.model.AnalyzeHostEntity;
import com.tencent.bk.codecc.schedule.model.AnalyzeHostPoolModel;
import com.tencent.bk.codecc.schedule.vo.PushVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.JsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = ScheduleBootApplication.class)
public class AnalyzeHostInit
{
    @Autowired
    private AnalyzeHostRepository analyzeHostRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

//    @Test
    public void initHostPool()
    {
        List<AnalyzeHostEntity> hostEntityList = analyzeHostRepository.findAll();
        Map<String, String> hostMap = new HashMap<>(hostEntityList.size());
        hostEntityList.forEach(hostEntity ->
        {
            int status = hostEntity.getStatus();
            if (ComConstants.Status.ENABLE.value() == status)
            {
                AnalyzeHostPoolModel hostPoolModel = new AnalyzeHostPoolModel();
                hostPoolModel.setIp(hostEntity.getIp());
                hostPoolModel.setPort(hostEntity.getPort());
                hostPoolModel.setPool(hostEntity.getThreadPool());
                hostPoolModel.setIdle(hostEntity.getThreadPool());
                hostPoolModel.setSupportTools(hostEntity.getSupportTools());
                hostPoolModel.setSupportTaskTypes(hostEntity.getSupportTaskTypes());
                hostMap.put(hostEntity.getIp(), JsonUtil.INSTANCE.toJson(hostPoolModel));
            }
        });
        if (hostMap.size() > 0)
        {
            redisTemplate.opsForHash().putAll(RedisKeyConstants.KEY_ANALYZE_HOST, hostMap);
        }
    }

//    @Test
    public void initHost()
    {
        String[] ips = {};

        long currTime = System.currentTimeMillis();

        for (String ip: ips)
        {
            AnalyzeHostEntity hostEntity = getHostEntity(currTime);
            hostEntity.setIp(ip);
            analyzeHostRepository.save(hostEntity);
        }
    }

    private AnalyzeHostEntity getHostEntity(long currTime)
    {
        AnalyzeHostEntity hostEntity = new AnalyzeHostEntity();
        hostEntity.setPort("8800");
        hostEntity.setThreadPool(30);
        hostEntity.setCores(90);
        hostEntity.setProcessors(90);
        hostEntity.setStatus(ComConstants.Status.ENABLE.value());

        Set<String> supportTools = new TreeSet<>();
        supportTools.add(ComConstants.Tool.COVERITY.name());
        supportTools.add(ComConstants.Tool.KLOCWORK.name());
        hostEntity.setSupportTools(supportTools);

        Set<String> supportTaskTypes = new TreeSet<>();
        supportTaskTypes.add(ComConstants.BsTaskCreateFrom.BS_PIPELINE.value());
        supportTaskTypes.add(ComConstants.BsTaskCreateFrom.BS_CODECC.value());
        hostEntity.setSupportTaskTypes(supportTaskTypes);

        hostEntity.setCreatedBy("xxxxx");
        hostEntity.setCreatedDate(currTime);
        hostEntity.setUpdatedBy("xxxxx");
        hostEntity.setUpdatedDate(currTime);
        return hostEntity;
    }

}