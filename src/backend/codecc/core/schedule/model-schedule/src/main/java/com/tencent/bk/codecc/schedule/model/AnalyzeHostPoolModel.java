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

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 分析服务器的实体类
 * 
 * @date 2019/11/4
 * @version V1.0
 */
@Data
public class AnalyzeHostPoolModel
{
    private String ip;

    private String port;

    /**
     * 线程池线程总数
     */
    private int pool;

    /**
     * 当前空闲线程数
     */
    private int idle;

    /**
     * 最大可执行线程（预留字段）
     */
    private int maxActive;

    /**
     * 支持的工具名
     */
    private Set<String> supportTools;

    /**
     * 支持的任务类型，当前主要用来区分是否支持开源扫描的
     */
    private Set<String> supportTaskTypes;

    /**
     * 当前正在执行的分析任务
     */
    private List<AnalyzeJob> jobList;

    @Data
    public static class AnalyzeJob
    {
        private String streamName;

        private String toolName;

        private String buildId;

        private String projectId;

        public AnalyzeJob()
        {
        }

        public AnalyzeJob(String streamName, String toolName, String buildId, String projectId)
        {
            this.streamName = streamName;
            this.toolName = toolName;
            this.buildId = buildId;
            this.projectId = projectId;
        }
    }
}
