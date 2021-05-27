package com.tencent.devops.common.api.checkerset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/1/8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckerSetVersionVO
{
    /**
     * 版本号
     */
    private Integer version;

    /**
     * 显示名称
     */
    private String displayName;
}
