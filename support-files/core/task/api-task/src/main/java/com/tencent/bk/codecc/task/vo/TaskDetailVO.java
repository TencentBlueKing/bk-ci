/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.vo;

import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务详细信息
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("任务详细信息")
public class TaskDetailVO extends TaskBaseVO
{
    @ApiModelProperty(value = "任务成员")
    private List<String> taskMember;

    /**
     * 已接入的所有工具名称，格式; COVERITY,CPPLINT,PYLINT
     */
    @ApiModelProperty(value = "已接入的所有工具名称")
    private String toolNames;

    @ApiModelProperty(value = "置顶标识")
    private Integer topFlag;

    /**
     * 项目接入的工具列表，查询时使用
     */
    @ApiModelProperty(value = "项目接入的工具列表")
    private List<ToolConfigInfoVO> toolConfigInfoList;

    @ApiModelProperty(value = "任务停用时间")
    private String disableTime;

    @ApiModelProperty(value = "编译平台")
    private String compilePlat;

    @ApiModelProperty(value = "运行平台")
    private String runPlat;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("蓝盾项目名称")
    private String projectName;

    @ApiModelProperty(value = "事业群id")
    private int bgId;

    @ApiModelProperty(value = "部门id")
    private int deptId;

    @ApiModelProperty(value = "中心id")
    private int centerId;

    @ApiModelProperty(value = "组id")
    private int groupId;

    @ApiModelProperty(value = "工作空间id")
    private long workspaceId;

    @ApiModelProperty(value = "凭证管理的主键id")
    private String repoHashId;

    @ApiModelProperty(value = "仓库别名")
    private String aliasName;

    @ApiModelProperty(value = "分支名，默认为master")
    private String branch;

    @ApiModelProperty(value = "代码库类型")
    private String scmType;

    @ApiModelProperty(value = "代码库的最新版本号")
    private String repoRevision;

    @ApiModelProperty(value = "将默认过滤路径放到任务实体对象下面")
    private List<String> defaultFilterPath;

    @ApiModelProperty(value = "已添加的自定义过滤路径")
    private List<String> filterPath;

    @ApiModelProperty(value = "code.yml自定义过滤路径")
    private List<String> testSourceFilterPath;

    @ApiModelProperty(value = "code.yml自定义过滤路径")
    private List<String> autoGenFilterPath;

    @ApiModelProperty(value = "code.yml自定义过滤路径")
    private List<String> thirdPartyFilterPath;

    @ApiModelProperty(value = "持续集成传递代码语言信息")
    private String devopsCodeLang;

    @ApiModelProperty("是否从工蜂创建")
    private Boolean gongfengFlag;

    @ApiModelProperty("工蜂项目id")
    private Integer gongfengProjectId;

    @ApiModelProperty("最近的commitId")
    private String gongfengCommitId;

    /**
     * 持续集成传递工具信息
     */
    @ApiModelProperty(value = "工具")
    private String devopsTools;

    @ApiModelProperty(value = "工具特定参数")
    private List<ToolConfigParamJsonVO> devopsToolParams;

    @ApiModelProperty(value = "编译类型")
    private String projectBuildType;

    @ApiModelProperty(value = "编译命令")
    private String projectBuildCommand;

    @ApiModelProperty(value = "操作系统类型")
    private String osType;

    @ApiModelProperty(value = "构建环境")
    private Map<String, String> buildEnv;

    @ApiModelProperty(value = "工具关联规则集")
    private List<ToolCheckerSetVO> toolCheckerSets;

    @ApiModelProperty(value = "工具列表")
    private Set<String> toolSet;

    @ApiModelProperty("最近分析时间")
    private Long minStartTime;

    /*----------------新任务页面显示-------------------*/
    @ApiModelProperty("显示工具")
    private String displayToolName;

    @ApiModelProperty("当前步骤")
    private Integer displayStep;

    @ApiModelProperty("步骤状态")
    private Integer displayStepStatus;

    @ApiModelProperty("显示进度条")
    private Integer displayProgress;

    @ApiModelProperty("显示工具信息")
    private String displayName;

    @ApiModelProperty("是否回写工蜂")
    private Boolean mrCommentEnable;

    /**
     * 是否是老插件切换为新插件，不对外接口暴露，仅用于内部逻辑参数传递
     */
    private boolean oldAtomCodeChangeToNew;

    /**
     * (谨供开源扫描注册用)是否强制更新项目规则集
     */
    private Boolean forceToUpdateOpenSource;

    /**
     * (谨供开源扫描注册用)配置项目规则集类型
     */
    private ComConstants.OpenSourceCheckerSetType openSourceCheckerSetType;

    public List<String> getAllFilterPaths()
    {
        List<String> filterPath = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(getFilterPath()))
        {
            filterPath.addAll(getFilterPath());
        }
        if(CollectionUtils.isNotEmpty(getDefaultFilterPath()))
        {
            filterPath.addAll(getDefaultFilterPath());
        }
        if(CollectionUtils.isNotEmpty(getTestSourceFilterPath()))
        {
            filterPath.addAll(getTestSourceFilterPath());
        }
        if(CollectionUtils.isNotEmpty(getAutoGenFilterPath()))
        {
            filterPath.addAll(getAutoGenFilterPath());
        }
        if(CollectionUtils.isNotEmpty(getThirdPartyFilterPath()))
        {
            filterPath.addAll(getThirdPartyFilterPath());
        }
        return filterPath;
    }
}
