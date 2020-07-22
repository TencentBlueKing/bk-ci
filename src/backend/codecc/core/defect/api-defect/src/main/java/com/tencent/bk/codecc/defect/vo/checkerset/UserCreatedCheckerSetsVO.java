package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户创建的规则集视图
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("用户创建的规则集视图")
public class UserCreatedCheckerSetsVO extends CommonVO
{
    /**
     * 用户创建的规则集列表
     */
    @ApiModelProperty(value = "规则集ID", required = true)
    private List<CheckerSetVO> userCreatedCheckerSets;
}
