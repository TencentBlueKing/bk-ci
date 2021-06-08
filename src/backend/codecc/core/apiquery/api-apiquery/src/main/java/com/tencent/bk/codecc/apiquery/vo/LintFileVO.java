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

package com.tencent.bk.codecc.apiquery.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * lint类文件告警详情
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@ApiModel("lint类文件告警详情")
public class LintFileVO {
    @ApiModelProperty(value = "任务id")
    private long taskId;

    @ApiModelProperty(value = "工具名称")
    private String toolName;

    @ApiModelProperty("文件名称")
    private String fileName;

    @ApiModelProperty("文件路径")
    private String filePath;

    @ApiModelProperty("文件更新时间")
    private long fileUpdateTime;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复
     */
    @ApiModelProperty("发现该告警的最近分析版本号")
    private String analysisVersion;

    /**
     * 状态：NEW(1), FIXED(2), PATH_MASK(8)
     */
    @ApiModelProperty(value = "状态", allowableValues = "{1,2,8}")
    private int status;

    @ApiModelProperty(value = "第一次检查出告警的时间")
    private long createTime;

    @ApiModelProperty(value = "文件被修复的时间")
    private long fixedTime;

    @ApiModelProperty(value = "告警被修复的时间")
    private long excludeTime;

    @ApiModelProperty(value = "本文件的告警总数，方便用于统计")
    private int defectCount;

    @ApiModelProperty(value = "本文件的新告警数，方便用于统计")
    private int newCount;

    @ApiModelProperty(value = "本文件的历史告警数，方便用于统计")
    private int historyCount;

    @ApiModelProperty(value = "代码库路径")
    private String url;

    @ApiModelProperty(value = "代码仓库id")
    private String repoId;

    @ApiModelProperty(value = "版本号")
    private String revision;

    @ApiModelProperty(value = "分支名称")
    private String branch;

    @ApiModelProperty(value = "相对路径")
    private String relPath;

    @ApiModelProperty(value = "代码库子模块")
    private String subModule;

    @ApiModelProperty(value = "作者清单")
    private Set<String> authorList;

    @ApiModelProperty(value = "规则清单")
    private Set<String> checkerList;

    @ApiModelProperty(value = "严重程度列表")
    private Set<Integer> severityList;

    /**
     * 文件所有告警的严重程度之和，用于排序
     */
    private int severity;

    /**
     * 告警清单
     */
    @ApiModelProperty(value = "告警清单")
    private List<LintDefectVO> defectList;


}
