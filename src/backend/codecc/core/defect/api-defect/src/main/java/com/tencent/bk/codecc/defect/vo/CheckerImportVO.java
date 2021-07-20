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

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 导入规则VO
 * 
 * @date 2020/4/8
 * @version V1.0
 */
@Data
@ApiModel("工具完整信息对象")
public class CheckerImportVO
{
    @ApiModelProperty(value = "工具名", required = true)
    @NotNull(message = "工具名不能为空")
    @Pattern(regexp = "[A-Z0-9\\-]+", message = "工具名称，只能包含大写字母")
    private String toolName;

    @ApiModelProperty(value = "规则列表", required = true)
    @NotEmpty(message = "规则列表不能为空")
    private List<CheckerDetailVO> checkerDetailVOList;

    @ApiModelProperty(value = "腾讯规范规则集列表")
    private List<CheckerSetVO> standardCheckerSetList;
}
