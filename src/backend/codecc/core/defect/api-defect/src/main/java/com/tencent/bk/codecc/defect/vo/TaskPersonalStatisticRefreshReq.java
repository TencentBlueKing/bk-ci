package com.tencent.bk.codecc.defect.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskPersonalStatisticRefreshReq {

    long taskId;

    String extraInfo;
}
