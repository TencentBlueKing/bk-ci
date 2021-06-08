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

import com.tencent.bk.codecc.apiquery.defect.model.IgnoreCheckerModel;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 配置忽略规则Dao
 *
 * @version V1.0
 * @date 2020/10/09
 */

@Repository
public class IgnoreCheckerDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION = "t_ignore_checker";


    /**
     * 查询工具的忽略规则
     *
     * @param taskId   任务ID
     * @param toolName 工具
     * @return model
     */
    public IgnoreCheckerModel findByTaskIdAndToolName(Long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId))
                .addCriteria(Criteria.where("tool_name").is(toolName));
        List<IgnoreCheckerModel> ignoreCheckerModels = mongoTemplate.find(query, IgnoreCheckerModel.class, COLLECTION);
        IgnoreCheckerModel model;
        if (CollectionUtils.isNotEmpty(ignoreCheckerModels)) {
            model = ignoreCheckerModels.get(0);
        } else {
            model = new IgnoreCheckerModel();
        }
        return model;
    }


}
