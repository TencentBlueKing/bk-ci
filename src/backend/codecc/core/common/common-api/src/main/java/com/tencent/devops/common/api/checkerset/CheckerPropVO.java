package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 规则集中规则与参数视图
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Data
@ApiModel("规则集中规则与参数视图")
public class CheckerPropVO
{
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 规则
     */
    private String checkerKey;

    /**
     * 规则名称
     */
    private String checkerName;

    /**
     * 规则参数
     */
    private String props;
}
