package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Data
public class QueryFileDefectVO
{
    /**
     * 告警所在文件表唯一ID
     */
    private String fileEntityId;

    /**
     * 告警ID
     */
    private String defectId;
}
