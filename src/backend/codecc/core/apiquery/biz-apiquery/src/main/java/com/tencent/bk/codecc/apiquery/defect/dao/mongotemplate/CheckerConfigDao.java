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

package com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate;

import com.tencent.bk.codecc.apiquery.defect.model.CheckerConfigModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 规则配置参数Dao
 *
 * @version V1.0
 * @date 2020/10/09
 */
@Repository
public class CheckerConfigDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 根据任务ID、工具查询规则配置
     *
     * @param taskId   任务ID
     * @param toolName 工具
     * @return list
     */
    public List<CheckerConfigModel> findByTaskIdAndToolName(Long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId))
                .addCriteria(Criteria.where("tool_name").is(toolName));
        return mongoTemplate.find(query, CheckerConfigModel.class, "t_checker_config");
    }


}
