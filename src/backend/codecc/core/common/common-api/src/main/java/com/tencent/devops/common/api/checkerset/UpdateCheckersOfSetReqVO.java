package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 更新规则集中的规则请求视图
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@ApiModel("更新规则集中的规则请求视图")
public class UpdateCheckersOfSetReqVO
{
    /**
     * 规则集中的规则
     */
    private List<CheckerPropVO> checkerProps;
}
