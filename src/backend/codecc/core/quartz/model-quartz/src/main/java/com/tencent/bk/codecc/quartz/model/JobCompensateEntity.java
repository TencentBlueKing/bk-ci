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

package com.tencent.bk.codecc.quartz.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * job补偿持久实体类
 *
 * @version V1.0
 * @date 2019/9/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "t_job_compensate")
public class JobCompensateEntity extends CommonEntity
{
    @Field("job_name")
    private String jobName;

    @Field("shard_tag")
    private String shardTag;

    @Field("schedule_execute_time")
    private String scheduleExecuteTime;

    @Field("has_compensated")
    private Boolean hasCompensated;
}
