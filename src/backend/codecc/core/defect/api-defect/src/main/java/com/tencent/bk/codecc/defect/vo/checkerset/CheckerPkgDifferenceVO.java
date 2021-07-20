package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 规则包差异视图
 *
 * @version V1.0
 * @date 2019/11/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则集视图")
public class CheckerPkgDifferenceVO extends CommonVO
{
    private String pkgName;

    private List<String> differentCheckers;
}
