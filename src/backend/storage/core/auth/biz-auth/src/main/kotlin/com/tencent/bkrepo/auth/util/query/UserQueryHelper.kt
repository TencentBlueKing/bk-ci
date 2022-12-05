package com.tencent.bkrepo.auth.util.query

import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.util.DataDigestUtils
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

object UserQueryHelper {

    fun buildPermissionCheck(userId: String, pwd: String, hashPwd: String): Query {
        val criteria = Criteria()
        criteria.orOperator(
            Criteria.where(TUser::pwd.name).`is`(hashPwd),
            Criteria.where("tokens.id").`is`(pwd)
        ).and(TUser::userId.name).`is`(userId)
        return Query.query(criteria)
    }

    fun filterNotLockedUser(): Query {
        return Query(Criteria(TUser::locked.name).`is`(false))
    }

    fun getUserById(userId: String): Query {
        val query = Query()
        return query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
    }

    fun getUserByIdAndPwd(userId: String, oldPwd: String): Query {
        return Query.query(
            Criteria().andOperator(
                Criteria.where(TUser::userId.name).`is`(userId),
                Criteria.where(TUser::pwd.name).`is`(DataDigestUtils.md5FromStr(oldPwd))
            )
        )
    }

    fun getUserByIdList(idList: List<String>): Query {
        val query = Query()
        return query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList))
    }

    fun getUserByIdAndRoleId(userId: String, roleId: String): Query {
        val query = Query()
        return query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId).and(TUser::roles.name).`is`(roleId))
    }

    fun getUserByIdListAndRoleId(idList: List<String>, roleId: String): Query {
        val query = Query()
        return query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList).and(TUser::roles.name).`is`(roleId))
    }

    fun getUserByName(userName: String?, admin: Boolean?, locked: Boolean?): Query {
        val criteria = Criteria()
        userName?.let {
            criteria.orOperator(
                Criteria.where(TUser::userId.name).regex("^$userName"),
                Criteria.where(TUser::name.name).regex("^$userName")
            )
        }
        admin?.let { criteria.and(TUser::admin.name).`is`(admin) }
        locked?.let { criteria.and(TUser::locked.name).`is`(locked) }
        return Query(criteria)
    }
}
