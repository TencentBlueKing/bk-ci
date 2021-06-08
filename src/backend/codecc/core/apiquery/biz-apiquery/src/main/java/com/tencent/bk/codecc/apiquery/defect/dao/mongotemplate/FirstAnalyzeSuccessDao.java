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


import com.tencent.bk.codecc.apiquery.defect.model.FirstAnalysisSuccessModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 首次分析成功Dao
 *
 * @version V1.0
 * @date 2020/9/24
 */
@Repository
public class FirstAnalyzeSuccessDao {

    @Autowired
    @Qualifier("defectMongoTemplate")
    private MongoTemplate mongoTemplate;


    /**
     * 查找工具首次分析成功时间
     *
     * @param taskId   任务ID
     * @param toolName 工具名
     * @return list
     */
    public List<FirstAnalysisSuccessModel> findByTaskIdAndToolName(Long taskId, String toolName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
        return mongoTemplate.find(query, FirstAnalysisSuccessModel.class, "t_first_analysis_success");
    }
}
