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

import com.tencent.bk.codecc.task.model.GrayToolProjectEntity;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * 灰度工具项目持久类
 * 
 * @date 2021/1/3
 * @version V1.0
 */
@Repository
public class GrayToolProjectDao 
{
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据project_id更新灰度项目记录
     * @param grayToolProjectEntity
     * @param user
     */
    public void upsertGrayToolProjectEntity(GrayToolProjectEntity grayToolProjectEntity,
                                            String user)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").is(grayToolProjectEntity.getProjectId()));
        Update update = new Update();
        update.set("project_id", grayToolProjectEntity.getProjectId());
        update.set("status", grayToolProjectEntity.getStatus());
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", user);
        update.set("created_date", System.currentTimeMillis());
        update.set("created_by", user);
        mongoTemplate.upsert(query, update, GrayToolProjectEntity.class);
    }

    /**
     * 分页查询灰度项目列表
     * @param reqVO
     * @param pageable
     */
    public Page<GrayToolProjectEntity> findGrayToolPage(GrayToolProjectVO reqVO, Pageable pageable) {
        Query query = new Query();

        String projectId = reqVO.getProjectId();
        if (projectId != null && ! projectId.isEmpty()) {
            query.addCriteria(Criteria.where("project_id").in(projectId));
        }

        String createBy = reqVO.getCreatedBy();
        if (createBy != null && ! createBy.isEmpty()) {
            query.addCriteria(Criteria.where("created_by").in(createBy));
        }

        String updateBy = reqVO.getUpdatedBy();
        if (updateBy != null && ! updateBy.isEmpty()) {
            query.addCriteria(Criteria.where("updated_by").in(updateBy));
        }

        if (pageable != null) {
            query.with(pageable);
        }

        List<GrayToolProjectEntity> grayToolProjectEntityList = mongoTemplate.find(query,
                                                                GrayToolProjectEntity.class, "t_gray_tool_project");

        return PageableExecutionUtils.getPage(
                grayToolProjectEntityList,
                pageable,
                () -> mongoTemplate.count(query.limit(-1).skip(-1), GrayToolProjectEntity.class));
    }

}
