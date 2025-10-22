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

package com.tencent.devops.common.webhook.enums.code.tgit

/*
 * operation_kind字段
 * create：创建分支、合并普通MR的push
 * delete：删除分支的push
 * update：文件修改的push
 * update_nonfastword：non-fast-forward提交
*/
enum class TGitPushOperationKind(val value: String) {
    CREAT("create"),
    DELETE("delete"),
    UPDATE("update"),
    UPDATE_NONFASTFORWORD("update_nonfastforward")
}

/*
 * action_kind字段
 * client push：客户端请求（不在工蜂Web上操作的都默认是这个）
 * create branch：在工蜂Web上创建分支
 * delete branch：在工蜂Web上删除分支
 * create tag：在工蜂Web上创建Tag
 * delete tag：在工蜂Web上删除Tag
 * create file：在工蜂Web上新建文件
 * modify file：在工蜂Web上修改文件
 * delete file：在工蜂Web上删除文件
 * replace file：在工蜂Web上替换文件
 * create a merge commit：在工蜂Web上合并普通MR
 * squash and merge：在工蜂Web上合并squash merge
 * rebase and merge：在工蜂Web上合并rebase merge
 * cherry-pick：在工蜂Web上使用cherry pick功能
 * revert：在工蜂Web上使用revert功能
 */
enum class TGitPushActionKind(val value: String) {
    CLIENT_PUSH("client push"),
    CREATE_BRANCH("create branch"),
    DELETE_BRANCH("delete branch"),
    CREATE_TAG("create tag"),
    DELETE_TAG("delete tag"),
    CREATE_FILE("create file"),
    MODIFY_FILE("modify file"),
    DELETE_FILE("delete file"),
    REPLACE_FILE("replace file"),
    CREATE_A_MERGE_COMMIT("create a merge commit"),
    SQUASH_AND_MERGE("squash and merge"),
    REBASE_AND_MERGE("rebase and merge"),
    CHERRY_PICK("cherry-pick"),
    REVERT("revert");

    companion object {
        /**
         * 将webhook中的动作类型转换成触发器JSON配置中的动作类型
         */
        fun convertActionType(value: String): TGitPushActionType {
            // webhook动作类型
            val pushActionKind = TGitPushActionKind.values().firstOrNull { it.value == value }
            return when (pushActionKind) {
                CREATE_BRANCH -> TGitPushActionType.NEW_BRANCH
                else -> TGitPushActionType.PUSH_FILE
            }
        }
    }
}

enum class TGitPushActionType(val value: String) {
    NEW_BRANCH("new-branch"),
    NEW_BRANCH_AND_PUSH_FILE("new-branch-and-push-file"),
    PUSH_FILE("push-file");
}
