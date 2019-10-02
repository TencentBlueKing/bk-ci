package com.tencent.devops.plugin.pojo.codecc

import com.fasterxml.jackson.annotation.JsonProperty

data class BlueShieldResponse(
    val res: Int,
    val msg: String,
    val data: List<Item>
) {
    data class Item(
        @JsonProperty("proj_id", required = false)
        val taskId: String,
        @JsonProperty("remain_defect_count", required = false)
        val remainCount: Int,
        @JsonProperty("repair_defect_count", required = false)
        val repairCount: Int,
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