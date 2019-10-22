package com.tencent.devops.common.pipeline.pojo.coverity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * deng
 * 30/01/2018
 *
 * {
 *   "task_info": {
 *     "task_id": "13320",
 *     "task_en_name": "LD_A7382F5B4F7ED9BE",
 *     "task_cn_name": "gaier_bs_test_033"
 *   },
 *   "res": 0,
 *   "msg": "register ok"
 *   }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoverityResult(
    val res: Int = 0,
    val msg: String = "no need register",
    val task_info: CoverityTaskInfo? = null
)