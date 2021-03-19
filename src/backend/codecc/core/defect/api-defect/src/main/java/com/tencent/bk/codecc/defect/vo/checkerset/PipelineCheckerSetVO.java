package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流水线规则集视图
 *
 * @version V1.0
 * @date 2019/11/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线规则集视图")
public class PipelineCheckerSetVO extends CommonVO
{
    private List<PipelineCheckerSetRecordVO> records;
}
