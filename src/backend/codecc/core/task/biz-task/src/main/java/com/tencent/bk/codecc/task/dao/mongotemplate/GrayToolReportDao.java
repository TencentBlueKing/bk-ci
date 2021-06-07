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
 
package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.GrayDefectTaskSubEntity;
import com.tencent.bk.codecc.task.model.GrayToolReportEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 灰度报告持久类
 * 
 * @date 2021/1/7
 * @version V1.0
 */
@Repository
public class GrayToolReportDao 
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 配置最近一次报告信息
     * @param codeccBuildId
     * @param defectCount
     */
    public void incrLastReportInfo(String codeccBuildId,
                                   Integer defectCount,
                                   Long elapsedTime,
                                   GrayDefectTaskSubEntity grayDefectTaskSubEntity)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("codecc_build_id").is(codeccBuildId));
        Update update = new Update();
        update.inc("last_report_info.total_num", 1);
        update.inc("current_report_info.total_num", 1);
        if (null != defectCount)
        {
            update.inc("last_report_info.success_num", 1);
            update.inc("last_report_info.defect_count", defectCount);
        }
        if (null != grayDefectTaskSubEntity) {
            update.push("defect_task_list", grayDefectTaskSubEntity);
        }
        if (null != elapsedTime)
        {
            update.inc("last_report_info.elapsed_time", elapsedTime);
        }
        mongoTemplate.updateFirst(query, update, GrayToolReportEntity.class);
    }

    /**
     * 配置当前报告信息
     * @param codeccBuildId
     * @param defectCount
     */
    public void incrCurrentReportInfo(String projectId,
                                      String codeccBuildId,
                                      Long taskId,
                                      Integer defectCount,
                                      Long elapsedTime,
                                      Boolean success)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").is(projectId))
                .addCriteria(Criteria.where("codecc_build_id").is(codeccBuildId))
                .addCriteria(Criteria.where("defect_task_list.task_id").is(taskId));
        Update update = new Update();
        if (null != defectCount)
        {
            update.inc("current_report_info.success_num", 1);
            update.inc("current_report_info.defect_count", defectCount);
            update.set("defect_task_list.$.current_defect_count", defectCount);
        }
        if (null != elapsedTime && null != success && success) {
            update.inc("current_report_info.elapsed_time", elapsedTime);
            update.set("defect_task_list.$.current_elapsed_time", elapsedTime);
        }
        update.set("defect_task_list.$.success", success);
        mongoTemplate.updateFirst(query, update, GrayToolReportEntity.class);
    }

}
