package com.tencent.devops.common.api.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 规则集分类视图
 *
 * @version V4.0
 * @date 2019/11/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集分类视图")
public class DividedCheckerSetsVO extends CommonVO
{
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 我的项目正在使用
     */
    private List<CheckerSetVO> myProjUse;

    /**
     * CodeCC推荐
     */
    private List<CheckerSetVO> recommended;

    /**
     * 其他
     */
    private List<CheckerSetVO> others;
}
