package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TSshPublicKeys
import com.tencent.devops.model.remotedev.tables.records.TSshPublicKeysRecord
import com.tencent.devops.remotedev.pojo.SshPublicKey
import org.jolokia.util.Base64Util
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.util.Base64
@Repository
class SshPublicKeysDao {

    // 新增ssh key
    fun createSshKey(
        dslContext: DSLContext,
        sshPublicKey: SshPublicKey,
    ): Long {
        // 先查询该用户SshPublicKey是否已存在，存在则返回已有ID，否则新增
        return getSshKeysRecord(dslContext, sshPublicKey)
            ?: with(TSshPublicKeys.T_SSH_PUBLIC_KEYS) {
                dslContext.insertInto(
                    this,
                    USER,
                    PUBLIC_KEY,
                ).values(
                    sshPublicKey.user,
                    Base64Util.encode(sshPublicKey.publicKey.toByteArray())
                ).returning(ID).fetchOne()!!.id
            }
    }

    // 获取当前用户的所有 ssh keys
    fun queryUserSshKeys(
        dslContext: DSLContext,
        users: Set<String>
    ): Result<TSshPublicKeysRecord> {
        with(TSshPublicKeys.T_SSH_PUBLIC_KEYS) {
            return dslContext.selectFrom(this)
                .where(USER.`in`(users))
                .fetch()
        }
    }

    // 判断某个给定的sshkey是否存在
    fun getSshKeysRecord(
        dslContext: DSLContext,
        sshPublicKey: SshPublicKey
    ): Long? {
        with(TSshPublicKeys.T_SSH_PUBLIC_KEYS) {
            return dslContext.selectFrom(this)
                .where(USER.eq(sshPublicKey.user))
                .and(PUBLIC_KEY.eq(Base64Util.encode(sshPublicKey.publicKey.toByteArray())))
                .fetchOne()?.id
        }
    }

}
