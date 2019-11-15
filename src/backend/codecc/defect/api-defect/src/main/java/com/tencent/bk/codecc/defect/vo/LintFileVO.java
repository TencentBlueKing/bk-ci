/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * lint类文件告警详情
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("lint类文件告警详情")
public class LintFileVO extends CommonVO
{

    /**
     * 任务id
     */
    @ApiModelProperty(value = "任务id")
    private long taskId;

    /**
     * 工具名称
     */
    @ApiModelProperty(value = "工具名称")
    private String toolName;


    /**
     * 文件路径
     */
    @ApiModelProperty("文件路径")
    private String filePath;

    /**
     * 文件更新时间
     */
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

    /**
     * 文件被修复的时间
     */
    @ApiModelProperty(value = "文件被修复的时间")
    private long fixedTime;

    /**
     * 告警被修复的时间
     */
    @ApiModelProperty(value = "告警被修复的时间")
    private long excludeTime;

    /**
     * 本文件的告警总数，等于defectList.size()，方便用于统计
     */
    @ApiModelProperty(value = "本文件的告警总数，等于defectList.size()，方便用于统计")
    private int defectCount;

    /**
     * 本文件的新告警数，方便用于统计
     */
    @ApiModelProperty(value = "本文件的新告警数，方便用于统计")
    private int newCount;

    /**
     * 本文件的历史告警数，方便用于统计
     */
    @ApiModelProperty(value = "本文件的历史告警数，方便用于统计")
    private int historyCount;

    /**
     * 代码库路径
     */
    @ApiModelProperty(value = "代码库路径")
    private String url;

    /**
     * 代码仓库id
     */
    @ApiModelProperty(value = "代码仓库id")
    private String repoId;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private String revision;

    /**
     * 分支名称
     */
    @ApiModelProperty(value = "分支名称")
    private String branch;

    /**
     * 相对路径
     */
    @ApiModelProperty(value = "相对路径")
    private String relPath;

    /**
     * 代码库子模块
     */
    @ApiModelProperty(value = "代码库子模块")
    private String subModule;

    /**
     * 作者清单
     */
    @ApiModelProperty(value = "作者清单")
    private Set<String> authorList;

    /**
     * 规则清单
     */
    @ApiModelProperty(value = "规则清单")
    private Set<String> checkerList;

    /**
     * 告警清单
     */
    @ApiModelProperty(value = "告警清单")
    private List<LintDefectVO> defectList;


}
