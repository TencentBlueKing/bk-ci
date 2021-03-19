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

package com.tencent.bk.codecc.apiquery.task.dao;

import com.tencent.bk.codecc.apiquery.task.model.ToolMetaEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 工具元数据持久
 * 
 * @date 2020/4/17
 * @version V1.0
 */
@Repository
public interface ToolMetaRepository extends MongoRepository<ToolMetaEntity, String>
{
    /**
     * 根据工具名查询元数据
     * @param toolName
     * @return
     */
    @Query(fields = "{'pattern' : 1}", value = "{'name' : ?0}")
    ToolMetaEntity findByName(String toolName);
}
