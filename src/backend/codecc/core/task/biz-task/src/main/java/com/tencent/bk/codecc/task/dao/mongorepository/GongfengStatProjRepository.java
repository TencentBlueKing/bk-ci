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

import com.tencent.bk.codecc.task.model.GongfengStatProjEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工蜂项目度量数据持久层
 *
 * @version V1.0
 * @date 2019/12/7
 */
@Repository
public interface GongfengStatProjRepository extends MongoRepository<GongfengStatProjEntity, String>
{
    /**
     * 批量获取工蜂项目度量数据
     *
     * @param bgId  事业群ID
     * @param idSet 工蜂id集合
     * @return list
     */
    List<GongfengStatProjEntity> findByBgIdIsAndIdIn(Integer bgId, Collection<Integer> idSet);

    /**
     * 通过gongfeng_id获取统计信息
     * @param gongfengId
     * @return
     */
    GongfengStatProjEntity findFirstById(Integer gongfengId);

}
