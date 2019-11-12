package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.TaskBootApplication;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.devops.common.constant.ComConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.util.Map;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = TaskBootApplication.class)
public class ToolMetaRepositoryTest
{
//    @Autowired
    private ToolMetaRepository toolMetaRepository;

//    @Test
    public void testAddCovPlatformInfo()
    {
        Jedis jedis177 = new Jedis("10.125.52.252", 50000);
        jedis177.auth("codecc1024");

        ToolMetaEntity spotbugs = toolMetaRepository.findByName(ComConstants.Tool.SPOTBUGS.name());

        Map<String, String> toolFieldMap = jedis177.hgetAll("TOOL:COVERITY");
        ToolMetaEntity toolMetaEntity = new ToolMetaEntity();
        toolMetaEntity.setName(toolFieldMap.get("name"));
        toolMetaEntity.setPattern(toolFieldMap.get("pattern"));
        toolMetaEntity.setDisplayName(toolFieldMap.get("displayName"));
        toolMetaEntity.setType(toolFieldMap.get("type"));
        toolMetaEntity.setLang(Long.valueOf(toolFieldMap.get("lang")));
        toolMetaEntity.setStatus(toolFieldMap.get("status"));
        toolMetaEntity.setParams(spotbugs.getParams());
        toolMetaEntity.setBriefIntroduction(toolFieldMap.get("briefIntroduction"));
        toolMetaEntity.setDescription(toolFieldMap.get("description"));
        toolMetaEntity.setPrivated(Boolean.valueOf(toolFieldMap.get("privated")));
        toolMetaEntity.setSyncLD(Boolean.valueOf(toolFieldMap.get("syncLD")));
        toolMetaEntity.setLogo(toolFieldMap.get("logo"));
        toolMetaEntity.setGraphicDetails(toolFieldMap.get("graphicDetails"));
        toolMetaEntity.setCreatedBy(toolFieldMap.get("creator"));
        toolMetaEntity.setCreatedDate(System.currentTimeMillis());
        toolMetaEntity.setUpdatedBy(toolFieldMap.get("lastUpdater"));
        toolMetaEntity.setUpdatedDate(System.currentTimeMillis());
        toolMetaRepository.save(toolMetaEntity);
    }

}