package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 特殊参数和规则集视图
 *
 * @version V1.0
 * @date 2019/11/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("特殊参数和规则集视图")
public class ParamJsonAndCheckerSetsVO extends CommonVO
{
    private List<ToolParamJsonAndCheckerSetVO> toolConfig;
}
