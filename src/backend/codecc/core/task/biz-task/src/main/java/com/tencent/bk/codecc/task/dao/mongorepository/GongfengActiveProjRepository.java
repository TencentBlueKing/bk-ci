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

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.GongfengActiveProjEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 工蜂货源项目持久代码
 *
 * @version V1.0
 * @date 2019/11/21
 */
@Repository
public interface GongfengActiveProjRepository extends MongoRepository<GongfengActiveProjEntity, String> {

    /**
     * 按id查询记录是否存在
     * @param id
     * @return
     */
    GongfengActiveProjEntity findFirstById(Integer id);
}
