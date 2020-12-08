package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 规则集中工具与规则列表视图
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Data
@ApiModel("规则集中工具与规则列表视图")
public class ToolCheckersVO
{
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 规则列表
     */
    private List<CheckerPropVO> checkers;
}
