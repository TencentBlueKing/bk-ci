package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 获取规则集列表请求体视图
 *
 * @version V4.0
 * @date 2019/10/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集视图")
public class GetCheckerSetsReqVO extends CommonVO
{
    @ApiModelProperty(value = "工具名称列表", required = true)
    private List<String> toolNames;
}
