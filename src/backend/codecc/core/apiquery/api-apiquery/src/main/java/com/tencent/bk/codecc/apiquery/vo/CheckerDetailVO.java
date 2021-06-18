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


import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * 规则详情视图实体类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则详情视图实体类")
public class CheckerDetailVO extends CommonVO {

    @ApiModelProperty(value = "工具名", required = true)
    private String toolName;

    @ApiModelProperty(value = "告警类型key", required = true)
    private String checkerKey;

    @ApiModelProperty(value = "规则名称", required = true)
    @Pattern(regexp = "^[a-zA-Z_]{1,20}$", message = "输入的规则名不符合命名规则")
    private String checkerName;

    @ApiModelProperty(value = "规则详细描述", required = true)
    @Size(max = 150)
    private String checkerDesc;

    @ApiModelProperty(value = "规则详细描述-带占位符")
    private String checkerDescModel;

    @ApiModelProperty(value = "规则严重程度，1=>严重，2=>一般，3=>提示", required = true, allowableValues = "{1,2,3}")
    @Min(1)
    @Max(3)
    private Integer severity;

    @ApiModelProperty(value = "规则所属语言（针对KLOCKWORK）", required = true)
    private Long language;

    @ApiModelProperty(value = "规则状态 2=>打开 1=>关闭;", required = true, allowableValues = "{1,2}")
    private Integer status;

    @ApiModelProperty("规则状态是否打开")
    private Boolean checkerStatus;

    @ApiModelProperty(value = "规则类型", required = true)
    private String checkerType;

    @ApiModelProperty(value = "规则类型说明", required = true)
    private String checkerTypeDesc;

    @ApiModelProperty(value = "规则类型排序序列号", required = true)
    private String checkerTypeSort;

    @ApiModelProperty(value = "所属规则包", required = true)
    private String pkgKind;

    @ApiModelProperty(value = "项目框架（针对Eslint工具,目前有vue,react,standard）", required = true)
    private String frameworkType;

    @ApiModelProperty(value = "规则配置", required = true)
    private String props;

    @ApiModelProperty(value = "规则参数值", required = true)
    private String paramValue;

    @ApiModelProperty(value = "规则所属标准", required = true)
    private Integer standard;

    @ApiModelProperty(value = "规则是否支持配置true：支持;空或false：不支持", required = true)
    private Boolean editable;

    @ApiModelProperty(value = "示例代码", required = true)
    private String codeExample;

    @ApiModelProperty(value = "是否原生规则true:原生;false:自定义")
    private Boolean nativeChecker;

    @ApiModelProperty(value = "是否进阶规则1:是;0:否")
    private int covProperty;

    @ApiModelProperty(value = "Coverity规则子选项")
    private List<CovSubcategoryVO> covSubcategory;

    @ApiModelProperty(value = "规则集是否选中")
    private Boolean checkerSetSelected;

    /*-------------------根据改动新增规则字段---------------------*/
    /**
     * 规则对应语言，都存文字，mongodb对按位与不支持
     */
    @ApiModelProperty(value = "规则对应语言，都存文字，mongodb对按位与不支持")
    private Set<String> checkerLanguage;

    /**
     * 规则类型
     */
    @ApiModelProperty(value = "规则类型")
    @Pattern(regexp = "CODE_DEFECT|CODE_FORMAT|SECURITY_RISK|COMPLEXITY|DUPLICATE")
    private String checkerCategory;

    /**
     * 规则类型中文名
     */
    @ApiModelProperty(value = "规则类型中文名")
    private String checkerCategoryName;

    /**
     * 规则标签
     */
    @ApiModelProperty(value = "规则标签")
    private Set<String> checkerTag;

    /**
     * 规则推荐类型
     */
    @ApiModelProperty(value = "规则推荐类型")
    private String checkerRecommend;

    @ApiModelProperty(value = "错误代码示例")
    private String errExample;

    @ApiModelProperty(value = "正确代码示例")
    private String rightExample;

    @ApiModelProperty(value = "规则值和工具名集合")
    private String checkerKeyAndToolName;

    @ApiModelProperty(value = "规则参数列表，规则导入时接口传入")
    private List<CheckerProps> checkerProps;
}
