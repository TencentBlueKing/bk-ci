package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 更新规则集基础信息请求体视图
 *
 * @version V1.0
 * @date 2020/1/7
 */
@Data
@ApiModel("更新规则集基础信息请求体视图")
public class V3UpdateCheckerSetReqVO
{
    /**
     * 规则集名称
     */
    private String checkerSetName;

    /**
     * 规则集描述
     */
    private String description;

    /**
     * 规则集类型列表
     */
    private List<String> catagories;
}
