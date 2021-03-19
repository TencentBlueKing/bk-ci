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

package com.tencent.bk.codecc.apiquery.defect.dao;

import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 重复率查询持久类
 * 
 * @author foxdd
 * @date 2020/4/18
 * @version V1.0
 */
@Repository
public interface DUPCDefectRepository extends MongoRepository<DUPCDefectEntity, String>
{

    /**
     * 根据任务id清单查询
     * @param taskIds
     * @param pageable
     * @return
     */
    @Query(fields = "{'url' : 0}", value = "{'task_id' : {$in : ?0}}")
    Page<DUPCDefectEntity> findByTaskIdInOrderByTaskIdAsc(List<Long> taskIds, Pageable pageable);
}
