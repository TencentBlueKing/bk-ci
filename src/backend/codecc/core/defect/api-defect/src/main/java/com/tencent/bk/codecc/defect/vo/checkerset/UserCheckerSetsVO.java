package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.DividedCheckerSetsVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户的规则集视图
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集视图")
public class UserCheckerSetsVO extends CommonVO
{
    /**
     * 工具对应的规则集
     */
    private List<DividedCheckerSetsVO> checkerSets;
}
