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
 
package com.tencent.bk.codecc.task.pojo;

import lombok.Data;

import java.util.Map;

/**
 * 工具最近一次分析结果
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@Data
public class LatestAnalysisResultModel 
{
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具当前执行步骤
     */
    private int curStep;

    /**
     * 工具当前步骤的状态
     */
    private int stepStatus;

    /**
     * 最近一次分析的告警统计信息,json格式的数据
     */
    private Map<String, Object> lastAnalyzeResult;

    /**
     * 最近一次成功分析的开始时间
     */
    private Long beginTime;

    /**
     * 最近一次成功分析的结束时间
     */
    private Long endTime;

    /**
     * 最近一次成功分析的耗时
     */
    private String timeConsuming;

    /**
     * coverity超过一定时间没有上传的标志，Y表示没有上传，N表示有上传
     */
    private String coverityNoUploadFlag;

    /**
     * coverity超过X天没有上传的
     */
    private int coverityNoUploadDays;

    /**
     * 建议值,true/false
     */
    private String dirStructSuggestParam;

    /**
     * 编译是否成功，true（成功）/false（失败）
     */
    private String compileResult;
}
