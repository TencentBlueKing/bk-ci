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

package com.tencent.bk.codecc.schedule.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * 分析服务器的实体类
 *
 * @date 2019/11/4
 * @version V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_analyze_host")
@CompoundIndexes({
        @CompoundIndex(name = "ip_1_port_1", def = "{'ip': 1, 'port': 1}")
})
public class AnalyzeHostEntity extends CommonEntity
{
    @Field("ip")
    private String ip;

    @Field("port")
    private String port;

    @Field("cores")
    private int cores;

    @Field("processors")
    private int processors;

    @Field("thread_pool")
    private int threadPool;

    @Field("support_tools")
    private Set<String> supportTools;

    /**
     * 支持的任务类型，当前主要用来区分是否支持开源扫描的
     */
    @Field("support_task_types")
    private Set<String> supportTaskTypes;

    /**
     * 0-启用，1-停用
     */
    @Field("status")
    private int status;
}
