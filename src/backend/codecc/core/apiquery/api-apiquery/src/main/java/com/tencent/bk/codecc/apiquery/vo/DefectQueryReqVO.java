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

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;


/**
 * 公共文件查询请求视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@ApiModel("公共文件查询请求视图")
public class DefectQueryReqVO {
    @ApiModelProperty("工具名")
    @NotNull(message = "工具名不能为空")
    protected String toolName;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("规则名")
    private String checker;

    @ApiModelProperty("作者")
    private String author;

    @ApiModelProperty(value = "严重程度：严重（1），一般（2），提示（4）", allowableValues = "{1,2,4}")
    private Set<String> severity;

    @ApiModelProperty(value = "告警状态：待修复（1），已修复（2），忽略（4），路径屏蔽（8），规则屏蔽（16）", allowableValues = "{1,2,4,8,16}")
    private Set<Integer> status;

    private Set<String> pkgChecker;

    @ApiModelProperty(value = "文件或路径列表")
    private Set<String> fileList;

    @ApiModelProperty(value = "规则包名")
    private String pkgId;

    @ApiModelProperty(value = "起始创建时间")
    private String startCreateTime;

    @ApiModelProperty(value = "截止创建时间")
    private String endCreateTime;

    @ApiModelProperty(value = "起始修复时间")
    private String startFixTime;

    @ApiModelProperty(value = "截止修复时间")
    private String endFixTime;

    @ApiModelProperty(value = "告警类型:新增(1),历史(2)", allowableValues = "{1,2}")
    private Set<String> defectType;

    @ApiModelProperty(value = "聚类类型:文件(file),问题(defect)", allowableValues = "{file,defect}")
    private String clusterType;

    @ApiModelProperty(value = "构建ID")
    private String buildId;

    @ApiModelProperty(value = "构建ID")
    private String lastId;

    @ApiModelProperty(value = "统计类型: 状态(STATUS), 严重程度(SEVERITY), 新旧告警(DEFECT_TYPE)")
    private String statisticType;

    @ApiModelProperty(value = "CLOC聚类类型：文件（FILE），语言（LANGUAGE）")
    private ComConstants.CLOCOrder order;

    @ApiModelProperty(value = "规则集列表")
    private CheckerSet checkerSet;

    @Data
    public static class CheckerSet {
        private String checkerSetId;

        private int version;
    }
}
