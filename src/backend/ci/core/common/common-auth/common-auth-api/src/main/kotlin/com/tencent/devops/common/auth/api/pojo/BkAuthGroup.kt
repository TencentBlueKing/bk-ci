/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.auth.api.pojo

/**
 * 项目角色组
 */
enum class BkAuthGroup(
    val value: String,
    val groupName: String,
    /*用于兼容v0的角色ID*/
    val roleId: Int? = null
) {
    /*项目用户组*/
    VISITOR("visitor", "访问者", 0), // 访问者
    CIADMIN("ciAdmin", "CI管理员", 1), // CI管理员
    MANAGER("manager", "管理员", 2), // 管理员
    DEVELOPER("developer", "开发人员", 4), // 开发人员
    MAINTAINER("maintainer", "运维人员", 5), // 运维人员
    TESTER("tester", "测试人员", 8), // 测试人员
    PM("pm", "产品人员", 6), // 产品人员
    QC("qc", "质量管理员", 7), // 质量管理员
    CI_MANAGER("ci_manager", "CI管理员", 9), // CI 管理员,流水线组及v0会使用到，新版RBAC废除
    GRADE_ADMIN("gradeAdmin", "分级管理员"), // 分级管理员
    CGS_MANAGER("cgs_manager", "云研发管理员"),

    /*其他资源用户组*/
    RESOURCE_MANAGER("manager", "拥有者"), // 拥有者
    EDITOR("editor", "编辑者"), // 编辑者
    EXECUTOR("executor", "执行者"), // 执行者
    VIEWER("viewer", "查看者"); // 查看者

    companion object {
        fun get(value: String): BkAuthGroup {
            values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("roleName($value) does not exist!")
        }

        fun contains(value: String): Boolean {
            values().forEach {
                if (value == it.value) return true
            }
            return false
        }

        fun getByRoleId(roleId: Int): BkAuthGroup {
            values().forEach {
                if (roleId == it.roleId) return it
            }
            throw IllegalArgumentException("roleId($roleId) does not exist!")
        }

        fun roleNameToRoleId(roleName: String): Int {
            return get(roleName).roleId!!
        }
    }
}
