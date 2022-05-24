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

import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;


/**
 * 工蜂公共项目持久代码
 *
 * @version V1.0
 * @date 2019/9/26
 */
@Component
public interface GongfengPublicProjRepository extends MongoRepository<GongfengPublicProjEntity, String>
{
    /**
     * 通过id查询工蜂开源项目信息
     * @param id
     * @return
     */
    GongfengPublicProjEntity findFirstById(Integer id);


    /**
     * 批量获取工蜂项目信息
     *
     * @param idSet 工蜂id集合
     * @return list
     */
    List<GongfengPublicProjEntity> findByIdIn(Collection<Integer> idSet);

    /**
     * 通过id进行删除
     * @param id
     */
    void deleteByIdIs(Integer id);

    /**
     * 判断指定工蜂ID的记录是否存在
     */
    Boolean existsById(Integer id);
}
