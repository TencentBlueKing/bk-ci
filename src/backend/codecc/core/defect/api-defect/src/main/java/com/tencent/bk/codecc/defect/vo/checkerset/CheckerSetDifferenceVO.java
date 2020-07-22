package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 规则集差异视图
 *
 * @version V1.0
 * @date 2019/11/21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集视图")
public class CheckerSetDifferenceVO extends CommonVO
{
    /**
     * 待比较源规则集
     */
    private Integer fromVersion;

    /**
     * 待比较目的规则集
     */
    private Integer toVersion;

    /**
     * 各规则包规则差异
     */
    private List<CheckerPkgDifferenceVO> packages;

    /**
     * 变更前规则数量
     */
    private Integer checkerCountFrom;

    /**
     * 变更后规则数量
     */
    private Integer checkerCountTo;

    /**
     * 添加了该规则集的我的任务数量
     */
    private Integer myTaskCount;

    /**
     * 新版本更新时间
     */
    private Long lastUpdateTime;
}
