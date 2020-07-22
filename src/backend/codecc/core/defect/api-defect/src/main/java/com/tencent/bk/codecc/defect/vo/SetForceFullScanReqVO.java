package com.tencent.bk.codecc.defect.vo;

import lombok.Data;

import java.util.List;

/**
 * 设置强制全量扫描标志请求体
 *
 * @version V1.0
 * @date 2020/3/10
 */
@Data
public class SetForceFullScanReqVO
{
    private String landunBuildId;
    private List<String> toolNames;
}
