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

import com.tencent.bk.codecc.apiquery.defect.model.CommonStatisticEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 缺陷类统计仓库
 * 
 * @author foxdd
 * @date 2020/4/18
 * @version V1.0
 */
@Repository
public interface CommonStatisticRepository extends MongoRepository<CommonStatisticEntity, String>
{

    /**
     * 通过任务清单查询缺陷类统计信息
     * @param taskIds
     * @return
     */
    Page<CommonStatisticEntity> findByTaskIdInOrderByTaskIdAsc(List<Long> taskIds, Pageable pageable);
}
