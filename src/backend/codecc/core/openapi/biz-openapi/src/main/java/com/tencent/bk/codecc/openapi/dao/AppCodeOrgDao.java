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

package com.tencent.bk.codecc.openapi.dao;

import com.tencent.bk.codecc.openapi.op.model.AppCodeOrgEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * appcode组织架构持久接口
 * 
 * @date 2020/5/15
 * @version V1.0
 */
@Repository
public interface AppCodeOrgDao extends MongoRepository<AppCodeOrgEntity, String>
{
    /**
     * 通过appcode查询组织结构权限信息
     * @param appCode
     * @return
     */
    AppCodeOrgEntity findFirstByAppCode(String appCode);
}
