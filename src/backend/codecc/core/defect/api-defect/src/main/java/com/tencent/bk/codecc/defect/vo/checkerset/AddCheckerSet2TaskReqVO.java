package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO;
import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 添加规则集到任务请求体视图
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("添加规则集到任务请求体视图")
public class AddCheckerSet2TaskReqVO extends CommonVO
{
    /**
     * 工具对应的规则集
     */
    private List<ToolCheckerSetVO> toolCheckerSets;

    /**
     * 是否通知任务拥有者 Y:是  N:否，默认为Y
     */
    private String remindTaskOwner;

    /**
     * 是否需要同步到流水线
     */
    private Boolean needUpdatePipeline;

    /**
     * 是否升级我的其他已关联该规则集的任务中的规则集   Y:是  N:否
     */
    private String upgradeCheckerSetOfUserTasks;

    /**
     * 提供不包含是否升级其他任务中的规则集选项的构造方法
     *
     * @param toolCheckerSets
     * @param remindTaskOwner
     * @param needUpdatePipeline
     */
    public AddCheckerSet2TaskReqVO(List<ToolCheckerSetVO> toolCheckerSets, String remindTaskOwner, Boolean needUpdatePipeline)
    {
        this.toolCheckerSets = toolCheckerSets;
        this.remindTaskOwner = remindTaskOwner;
        this.needUpdatePipeline = needUpdatePipeline;
        this.upgradeCheckerSetOfUserTasks = ComConstants.CommonJudge.COMMON_N.value();
    }
}
