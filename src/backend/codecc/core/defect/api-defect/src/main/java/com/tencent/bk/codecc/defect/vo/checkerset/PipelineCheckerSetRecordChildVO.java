package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线规则集记录子节点视图
 *
 * @version V1.0
 * @date 2019/11/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线规则集记录子节点视图")
public class PipelineCheckerSetRecordChildVO extends CommonVO
{
    private String ruleSetId;
    private String ruleSetName;
    private List<String> codeLangs;
    private String eslintRc;
    private String phpcsStandard;
}
