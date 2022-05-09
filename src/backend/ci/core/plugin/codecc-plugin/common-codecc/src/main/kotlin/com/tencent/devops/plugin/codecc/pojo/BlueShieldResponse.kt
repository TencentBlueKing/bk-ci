/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.codecc.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class BlueShieldResponse(
    val res: Int,
    val msg: String,
    val data: List<Item>
) {
    data class Item(
        @ApiModelProperty(name = "proj_id")
        @JsonProperty("proj_id", required = false)
        val taskId: String,
        @ApiModelProperty(name = "remain_defect_count")
        @JsonProperty("remain_defect_count", required = false)
        val remainCount: Int,
        @ApiModelProperty(name = "repair_defect_count")
        @JsonProperty("repair_defect_count", required = false)
        val repairCount: Int,
        @ApiModelProperty(name = "repair_defect_serious_map")
        @JsonProperty("repair_defect_serious_map", required = false)
        val seriousMap: MutableMap<String, Int>
    )
}

/**
 * {
"res": 0,
"msg": "query success!",
"data": [
{
"proj_id": "13525",
"remain_defect_count": 34,
"repair_defect_count": 14,
"repair_defect_serious_map": {
"UNINIT": 1,
"OVERRUN": 1,
"NEGATIVE_RETURNS": 1
}
},
{
"proj_id": "13664",
"remain_defect_count": 0,
"repair_defect_count": 0,
"repair_defect_serious_map": {}
},
{
"proj_id": "11122222221111",
"remain_defect_count": 0,
"repair_defect_count": 0,
"repair_defect_serious_map": {}
}
]
}
 *
 *
 */
