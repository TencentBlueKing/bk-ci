package com.tencent.devops.experience.pojo.group

enum class GroupMemberType(val id: Int) {
    INNER(1),
    OUTER(2),
    DEPT(3);

    fun eq(id: Int): Boolean {
        return this.id == id
    }
}
