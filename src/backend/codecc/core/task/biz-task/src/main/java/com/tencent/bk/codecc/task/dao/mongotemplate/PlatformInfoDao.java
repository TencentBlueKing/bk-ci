package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.task.model.PlatformInfoEntity;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Platform信息持久层代码
 *
 * @version V2.0
 * @date 2020/1/11
 */
@Repository
public class PlatformInfoDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 按条件获取Platform信息
     *
     * @param toolName   工具名
     * @param platformIp ip
     * @return list
     */
    public List<PlatformInfoEntity> queryEntity(String toolName, String platformIp)
    {
        Document fieldsObj = new Document();
        fieldsObj.put("tool_name", true);
        fieldsObj.put("ip", true);
        fieldsObj.put("port", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        if (StringUtils.isNotEmpty(toolName))
        {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }

        if (StringUtils.isNotEmpty(platformIp))
        {
            query.addCriteria(Criteria.where("ip").is(platformIp));
        }

        return mongoTemplate.find(query, PlatformInfoEntity.class);
    }

}
