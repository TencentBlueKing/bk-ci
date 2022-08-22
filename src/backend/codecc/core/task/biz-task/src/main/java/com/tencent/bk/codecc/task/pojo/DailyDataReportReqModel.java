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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.bk.codecc.task.vo.TaskOverviewVO;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生成日报html和图片的请求对象
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@Data
public class DailyDataReportReqModel 
{
    /**
     * 请求类型，定时报告-T （timing），即时报告-I  （immediate）,所有工具即时报告-A(ALL) added by 20190326 新增所有工具即时报告
     */
    private String type;

    @JsonProperty("projId")
    private Long taskId;

    @JsonProperty("bs_project_id")
    private String projectId;

    /**
     * 流名称
     */
    private String streamName;

    /**
     * 中文名
     */
    private String nameCN;

    /**
     * 平台页面url的根节点，如http://test2.codecc.oa.com
     */
    private String urlRoot;

    /**
     * 各工具最近一次分析报告的汇总
     */
    private List<TaskOverviewVO.LastAnalysis> summary;

    private List<NodeDataReportReqModel> toolsData;

    /**
     * 流水线构建id
     */
    private String bsBuildId;

    /**
     * 代码库地址集，分Cov或Kloc 和普通工具
     * 返回如下的结构：
     * [
     *     { url: 'xxx.git', toolNames: ['Cov','Kloc'], branch: 'xx' }
     *     { url: 'xx.git', toolNames: ['ESLint','PyLint','ddd'], branch: 'xx' }
     * ]
     */
    private Set<CodeRepoVO> repoUrls;

    /**
     * 工具基本信息的映射表
     */
    private Map<String, ToolBaseInfoModel> toolInfoMap;

    /**
     * 创建来源
     */
    private String createFromCn;
}
