/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.mongodb.WriteResult;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.model.CLOCDefectEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * cloc信息持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Component
public class CLOCDefectDao
{
    @Autowired
    private MongoTemplate mongoTemplate;


    public WriteResult upsertCLOCInfoByFileName(CLOCDefectEntity clocDefectEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(clocDefectEntity.getTaskId()))
                .addCriteria(Criteria.where("file_name").is(clocDefectEntity.getFileName()));

        Update update = new Update();
        update.set("task_id", clocDefectEntity.getTaskId())
                .set("stream_name", clocDefectEntity.getStreamName())
                .set("file_name", clocDefectEntity.getFileName())
                .set("tool_name", clocDefectEntity.getToolName())
                .set("blank", clocDefectEntity.getBlank())
                .set("code", clocDefectEntity.getCode())
                .set("comment", clocDefectEntity.getComment())
                .set("language", clocDefectEntity.getLanguage());
        return mongoTemplate.upsert(query, update, CLOCDefectEntity.class);
    }

    /**
     * 批量失效
     * @param taskId
     */
    public void batchDisableClocInfo(Long taskId)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        Update update = new Update();
        update.set("status", "DISABLED");
        mongoTemplate.updateMulti(query, update, CLOCDefectEntity.class);
    }

    /**
     * 批量失效指定文件告警
     *
     * @param taskId 任务ID
     * @param fileNames 文件全路径
     * */
    public void batchDisableClocInfoByFileName(Long taskId, List<String> fileNames) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("file_name").in(fileNames));
        Update update = new Update();
        update.set("status", "DISABLED");
        mongoTemplate.updateMulti(query, update, CLOCDefectEntity.class);
    }

    /**
     * 批量更新写入指定文件告警
     * @param clocDefectEntityList 告警列表
     * */
    public void batchUpsertClocInfo(List<CLOCDefectEntity> clocDefectEntityList) {
        if (CollectionUtils.isNotEmpty(clocDefectEntityList))
        {
            BulkOperations operations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CLOCDefectEntity.class);
            clocDefectEntityList.forEach(clocDefectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(clocDefectEntity.getTaskId()))
                        .addCriteria(Criteria.where("file_name").is(clocDefectEntity.getFileName()));

                Update update = new Update();
                update.set("task_id", clocDefectEntity.getTaskId())
                        .set("stream_name", clocDefectEntity.getStreamName())
                        .set("file_name", clocDefectEntity.getFileName())
                        .set("tool_name", clocDefectEntity.getToolName())
                        .set("blank", clocDefectEntity.getBlank())
                        .set("code", clocDefectEntity.getCode())
                        .set("comment", clocDefectEntity.getComment())
                        .set("language", clocDefectEntity.getLanguage())
                        .set("status", "ENABLED");
                operations.upsert(query, update);
            });
            operations.execute();
        }
    }


    /**
     * 查询代码行数信息
     * @param taskId
     * @return
     */
    public List<CodeLineModel> getCodeLineInfo(Long taskId) {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").is(taskId));
        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("language").sum("code").as("codeLine");

        ProjectionOperation project = Aggregation.project().andExpression("_id").as("language").
                andExpression("codeLine").as("codeLine");
        //聚合配置
        Aggregation agg = Aggregation.newAggregation(match, group, project);

        AggregationResults<CodeLineModel> queryResult = mongoTemplate.aggregate(agg, "t_cloc_defect", CodeLineModel.class);
        return queryResult.getMappedResults();
    }

}
