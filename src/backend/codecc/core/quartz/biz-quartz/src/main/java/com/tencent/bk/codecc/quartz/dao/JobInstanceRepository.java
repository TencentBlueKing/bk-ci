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

package com.tencent.bk.codecc.quartz.dao;

import com.tencent.bk.codecc.quartz.model.JobInstanceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务实例表
 *
 * @version V1.0
 * @date 2019/9/17
 */
@Component
public interface JobInstanceRepository extends MongoRepository<JobInstanceEntity, String>
{

    /**
     * 通过job名字进行查询
     *
     * @param jobName
     * @return
     */
    JobInstanceEntity findFirstByJobName(String jobName);

    /**
     * 通过job名字范围进行查询
     *
     * @param jobNames
     * @return
     */
    List<JobInstanceEntity> findByJobNameIn(List<String> jobNames);

    /**
     * 根据job名字删除
     *
     * @param jobName
     */
    void deleteByJobName(String jobName);

    /**
     * 通过class_name来查找
     * @param className
     * @return
     */
    List<JobInstanceEntity> findByClassName(String className);


}
