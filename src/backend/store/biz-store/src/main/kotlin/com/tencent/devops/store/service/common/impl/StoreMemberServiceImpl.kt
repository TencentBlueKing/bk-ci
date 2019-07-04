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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.atom.AtomMemberItem
import com.tencent.devops.store.pojo.atom.AtomMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMemberService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Service
class StoreMemberServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao
) : StoreMemberService {

    /***
     * store组件成员列表
     */
    override fun list(userId: String, atomCode: String, storeType: StoreTypeEnum): Result<List<AtomMemberItem?>> {
        if (!storeMemberDao.isStoreMember(dslContext, userId, atomCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }

        val records = storeMemberDao.list(dslContext, atomCode, null, storeType.type.toByte())

        val members = mutableListOf<AtomMemberItem?>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        records?.forEach {
            members.add(
                AtomMemberItem(
                    id = it.id as String,
                    userName = it.username as String,
                    type = StoreMemberTypeEnum.getAtomMemberType((it.type as Byte).toInt()),
                    creator = it.creator as String,
                    modifier = it.modifier as String,
                    createTime = df.format(it.createTime as TemporalAccessor),
                    updateTime = df.format(it.updateTime as TemporalAccessor)
                )
            )
        }

        return Result(members)
    }

    override fun batchListMember(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<String>>> {
        val ret = hashMapOf<String, MutableList<String>>()
        val records = storeMemberDao.batchList(dslContext, storeCodeList, storeType.type.toByte())
        records?.forEach {
            val list = if (ret.containsKey(it["STORE_CODE"] as String)) {
                ret[it["STORE_CODE"] as String]!!
            } else {
                val tmp = mutableListOf<String>()
                ret[it["STORE_CODE"] as String] = tmp
                tmp
            }
            list.add(it["USERNAME"] as String)
        }
        return Result(ret)
    }

    /**
     * 添加store组件成员
     */
    override fun add(userId: String, atomMemberReq: AtomMemberReq, storeType: StoreTypeEnum): Result<Boolean> {
        val atomCode = atomMemberReq.atomCode
        val type = atomMemberReq.type.type.toByte()

        if (!storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }

        for (item in atomMemberReq.member) {
            if (storeMemberDao.isStoreMember(dslContext, item, atomCode, storeType.type.toByte())) {
                continue
            }
            storeMemberDao.addStoreMember(dslContext, userId, atomCode, item, type, storeType.type.toByte())
        }

        return Result(true)
    }

    /**
     * 删除store组件成员
     */
    override fun delete(userId: String, id: String, atomCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, atomCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }

        val record = storeMemberDao.getById(dslContext, id)

        if (record != null) {
            if ((record["TYPE"] as Byte).toInt() == 0) {
                val adminCount = storeMemberDao.countAdmin(dslContext, atomCode, storeType.type.toByte())
                if (adminCount <= 1) {
                    return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_COMPONENT_ADMIN_COUNT_ERROR)
                }
            }

            storeMemberDao.delete(dslContext, id)
        }

        return Result(true)
    }
}