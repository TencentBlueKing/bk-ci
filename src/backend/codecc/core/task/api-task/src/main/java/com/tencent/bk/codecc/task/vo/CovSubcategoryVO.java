package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则子选项
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则子选项视图实体类")
public class CovSubcategoryVO extends CommonVO
{
    @ApiModelProperty(value = "规则子选项唯一标识")
    private String checkerSubcategoryKey;

    @ApiModelProperty(value = "规则资源向名称")
    private String checkerSubcategoryName;

    @ApiModelProperty(value = "规则子选项详情")
    private String checkerSubcategoryDetail;

    @ApiModelProperty(value = "规则名唯一标识")
    private String checkerKey;

    @ApiModelProperty(value = "工具可识别规则名")
    private String checkerName;

    @ApiModelProperty(value = "语言")
    private int language;
}
