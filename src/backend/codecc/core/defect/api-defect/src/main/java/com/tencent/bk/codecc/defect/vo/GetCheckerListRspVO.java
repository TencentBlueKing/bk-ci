package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 查询规则列表响应体视图
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Data
@ApiModel("代码重复率的扫描统计结果")
public class GetCheckerListRspVO
{
    private List<CheckerPkgRspVO> checkerPackages;

    private CheckerSetVO checkerSet;
}
