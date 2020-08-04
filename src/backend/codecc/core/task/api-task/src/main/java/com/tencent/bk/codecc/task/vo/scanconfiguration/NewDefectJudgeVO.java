package com.tencent.bk.codecc.task.vo.scanconfiguration;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 新告警判定视图
 *
 * @version V4.0
 * @date 2019/11/8
 */
@Data
@ApiModel("新告警判定视图")
public class NewDefectJudgeVO extends CommonVO
{
    /**
     * 判定方式1：按日期；2：按构建
     */
    private Integer judgeBy;

    /**
     * 新告警开始的日期
     */
    private String fromDate;

    /**
     * 新告警开始的日期时间戳
     */
    private Long fromDateTime;

    /**
     * 最近几次构建产生的告警为新告警
     */
    private Integer lastestTimes;
}
