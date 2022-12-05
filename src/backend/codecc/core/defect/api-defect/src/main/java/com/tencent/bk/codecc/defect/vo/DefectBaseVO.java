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

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 告警基础信息VO
 *
 * @version V1.0
 * @date 2019/10/18
 */
@Data
@ApiModel("告警基础信息视图")
public class DefectBaseVO extends CommonVO
{
    @ApiModelProperty(value = "告警的唯一标志", required = true)
    private String id;

    @ApiModelProperty(value = "哈希Id", required = true)
    private String hashId;

    @ApiModelProperty(value = "任务ID", required = true)
    private long taskId;

    @ApiModelProperty(value = "流名称", required = true)
    private String streamName;

    @ApiModelProperty(value = "工具名", required = true)
    private String toolName;

    @ApiModelProperty(value = "规则名称", required = true)
    private String checkerName;

    @ApiModelProperty(value = "规则中文名称", required = true)
    private String checkerNameCn;

    @ApiModelProperty(value = "描述", required = true)
    private String description;

    /**
     * 用户给告警标志的状态,这个状态采用自定义的状态，而不使用klocwork的状态
     * 1:待处理(默认)，4:已忽略，8:路径屏蔽，16:规则屏蔽，32:标志位已修改
     */
    @ApiModelProperty(value = "状态", required = true)
    private int status;

    @ApiModelProperty(value = "告警所在文件", required = true)
    private String filePathname;

    @ApiModelProperty(value = "文件MD5")
    private String fileMD5;

    @ApiModelProperty(value = "文件名", required = true)
    private String fileName;

    /**
     * 规则类型，对应Coverity Platform中的Category(类别)
     */
    @ApiModelProperty(value = "规则类型", required = true)
    protected String displayCategory;

    /**
     * 类型子类，对应Coverity Platform中的Type(类型)
     */
    @ApiModelProperty(value = "类型子类", required = true)
    protected String displayType;

    @ApiModelProperty(value = "告警处理人", required = true)
    private Set<String> authorList;

    @ApiModelProperty(value = "告警严重程度", required = true)
    private int severity;

    @ApiModelProperty(value = "告警行号", required = true)
    protected int lineNumber;

    @ApiModelProperty(value = "忽略告警原因类型")
    private int ignoreReasonType;

    @ApiModelProperty(value = "忽略告警具体原因")
    private String ignoreReason;

    @ApiModelProperty(value = "忽略告警的作者")
    private String ignoreAuthor;

    @ApiModelProperty(value = "告警创建时间")
    private long createTime;

    @ApiModelProperty(value = "告警修复时间")
    private long fixedTime;

    @ApiModelProperty(value = "告警忽略时间")
    private long ignoreTime;

    @ApiModelProperty(value = "告警屏蔽时间")
    private long excludeTime;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @ApiModelProperty(value = "告警是否被标记为已修改的标志")
    private Integer mark;

    @ApiModelProperty(value = "告警被标记为已修改的时间")
    private Long markTime;

    @ApiModelProperty(value = "被处理的时间，包括closed,excluded,ignore")
    private String offTime;

    @ApiModelProperty(value = "创建时的构建号")
    private String createBuildNumber;

    @ApiModelProperty(value = "修复时的构建号")
    private String fixedBuildNumber;

    @ApiModelProperty(value = "文件对应仓库版本号")
    private String fileVersion;

    /**
     * 对应第三方缺陷管理系统的ID，这里声明为字符串可以有更好的兼容性
     */
    private String extBugid;

    /*--------------添加代码库信息 start----------------*/

    @ApiModelProperty("代码库id")
    private String repoId;

    @ApiModelProperty("版本号")
    private String revision;

    @ApiModelProperty("分支")
    private String branch;

    @ApiModelProperty("相对路径")
    private String relPath;

    @ApiModelProperty("代码库路径")
    private String url;

    /*--------------添加代码库信息 end----------------*/
}
