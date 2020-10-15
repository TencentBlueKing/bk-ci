package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 工具构建信息DAO
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Repository
@Slf4j
public class ToolBuildInfoDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新
     *
     * @param taskId
     * @param toolName
     * @param forceFullScan
     * @param baseBuildId
     */
    public void upsert(long taskId, String toolName, String forceFullScan, String baseBuildId)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName));

        Update update = new Update();
        update.set("task_id", taskId)
                .set("tool_name", toolName);
        if (StringUtils.isNotEmpty(forceFullScan))
        {
            update.set("force_full_scan", forceFullScan);
        }
        if (StringUtils.isNotEmpty(baseBuildId))
        {
            update.set("defect_base_build_id", baseBuildId);
        }

        mongoTemplate.upsert(query, update, ToolBuildInfoEntity.class);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void setForceFullScan(long taskId, String toolName)
    {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_Y.value(), null);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void clearForceFullScan(long taskId, String toolName)
    {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_N.value(), null);
    }

    /**
     * 更新告警快照基准构建ID
     *
     * @param taskId
     * @param toolName
     * @param buildId
     */
    public void updateDefectBaseBuildId(long taskId, String toolName, String buildId)
    {
        upsert(taskId, toolName, null, buildId);
    }
}
