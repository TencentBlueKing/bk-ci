package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/7
 */
@Data
@ApiModel("规则集视图")
public class CreateCheckerSetReqVO
{
    /**
     * 规则集ID
     */
    @ApiModelProperty(value = "规则集ID", required = true)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,50}$", message = "输入的规则集ID不符合命名规则")
    private String checkerSetId;

    /**
     * 规则集名称
     */
    @ApiModelProperty(value = "规则集名称", required = true)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,50}", message = "输入的规则集名称不符合命名规则")
    private String checkerSetName;

    /**
     * 规则集支持的语言
     */
    private Long codeLang;

    /**
     * 规则类型
     */
    private List<String> catagories;

    /**
     * 规则集描述
     */
    private String description;

    /**
     * 基准规则集ID
     */
    private String baseCheckerSetId;

    /**
     * 基准规则集版本号
     */
    private Integer baseCheckerSetVersion;

    /**
     * 版本号
     */
    private Integer version;
}
