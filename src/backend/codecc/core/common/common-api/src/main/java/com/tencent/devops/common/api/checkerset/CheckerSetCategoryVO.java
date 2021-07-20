package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 规则集类型视图
 *
 * @version V1.0
 * @date 2020/1/6
 */
@Data
@ApiModel("规则集类型视图")
public class CheckerSetCategoryVO
{
    /**
     * 英文名称
     */
    private String enName;

    /**
     * 中文名称
     */
    private String cnName;
}
