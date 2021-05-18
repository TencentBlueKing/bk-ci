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

import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务持久类
 * 
 * @date 2020/4/17
 * @version V1.0
 */
@Repository
public interface TaskRepository extends MongoRepository<TaskInfoModel, String>
{

    /**
     * 通过bd_id进行查找
     * @param bgId
     * @return
     */
    @Query(value = "{'bg_id' : ?0, 'create_from': 'gongfeng_scan', 'custom_proj_info' : null}")
    List<TaskInfoModel> findTegByBgId(Integer bgId);
}
