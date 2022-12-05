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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

/**
 * 屏蔽路径树输入参数实体
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Data
@ApiModel("屏蔽路径树输入参数视图")
public class FilterPathInputVO
{

    @NotNull(message = "任务ID不能为空")
    @ApiModelProperty(value = "任务ID", required = true)
    private Long taskId;

    @ApiModelProperty(value = "默认过滤路径文件")
    private List<String> defaultFilterPath;

    @ApiModelProperty(value = "自定义过滤路径文件")
    private List<String> filterFile;

    @ApiModelProperty(value = "自定义过滤路径文件夹")
    private List<String> filterDir;

    @ApiModelProperty(value = "手动输入的自定义过滤路径")
    private List<String> customPath;

    @NotNull(message = "过滤路径类型不能为空")
    @ApiModelProperty(value = "过滤路径类型")
    private String pathType;

    @ApiModelProperty(value = "是否为添加屏蔽路径")
    @JsonIgnore
    private Boolean addFile;

    @ApiModelProperty(value = "用户名称")
    @JsonIgnore
    private String userName;

    @ApiModelProperty(value = "后台整理后的过滤路径")
    private Set<String> filterPaths;

    @ApiModelProperty(value = "生效的工具列表")
    private List<String> effectiveTools;

    @ApiModelProperty(value = "工具名")
    private String toolName;

    private List<String> testSourceFilterPath;

    private List<String> autoGenFilterPath;

    private List<String> thirdPartyFilterPath;

    /*
     * 是否扫描测试代码，true-扫描，false-不扫描，默认不扫描
     */
    private Boolean scanTestSource;

    /**
     * 代码规范工具
     */
    private Set<String> standardToolSet;
}
