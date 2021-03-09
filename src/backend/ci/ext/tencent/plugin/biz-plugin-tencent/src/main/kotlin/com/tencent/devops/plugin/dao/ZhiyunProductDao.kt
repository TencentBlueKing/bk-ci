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

package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginZhiyunProduct
import com.tencent.devops.model.plugin.tables.records.TPluginZhiyunProductRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ZhiyunProductDao {

    fun getList(
        dslContext: DSLContext
    ): Result<TPluginZhiyunProductRecord>? {
        with(TPluginZhiyunProduct.T_PLUGIN_ZHIYUN_PRODUCT) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }

    fun save(
        dslContext: DSLContext,
        productId: String,
        productName: String
    ) {
        with(TPluginZhiyunProduct.T_PLUGIN_ZHIYUN_PRODUCT) {
            dslContext.insertInto(this,
                    PRODUCT_ID,
                    PRODUCT_NAME,
                    CREATED_TIME,
                    UPDATED_TIME)
                    .values(
                        productId,
                        productName,
                        LocalDateTime.now(),
                        LocalDateTime.now())
                    .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        productId: String
    ) {
        with(TPluginZhiyunProduct.T_PLUGIN_ZHIYUN_PRODUCT) {
            dslContext.delete(this).where(PRODUCT_ID.eq(productId)).execute()
        }
    }
}

/*

DROP TABLE IF EXISTS `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT`;
CREATE TABLE `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PRODUCT_ID` varchar(128) NOT NULL,
  `PRODUCT_NAME` varchar(128) NOT NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PRODUCT_ID` (`PRODUCT_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8;


  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("devtest","开发测试", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qzone","QZone", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqshow","QQ秀", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("xiaoyou","校友", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("club","会员", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("public","公共组件", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("other","其他", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqlive","QQLive", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("music","音乐", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("disk","网络硬盘", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("photo","相册", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("oss","OSS", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("MP","营销平台", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("video","Video", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("love","交友", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("taotao","滔滔", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qbar","QBar", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("P2P","P2P", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("third","第三方合作", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("cloud","云运营平台", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("NBAVIP","NBAVIP", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqconnect","QQ互联", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("weiyun","微云", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("gdt","广点通", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("xuanfeng","旋风", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("QQ","QQ", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM","IM", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("KT","客厅产品", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_Lconn","IM-长连接", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_Extension","IM-扩展类", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_DB","IM-DB", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_MobileQQ","IM-手机QQ", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_CONN","IM-CONN", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_authentication","IM-鉴权平台", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_Group","IM-群", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_CRM","IM-CRM", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("youtu","优图", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("IM_Grocery","IM-Grocery", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("c2b","QQ公众号", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqvac","增值渠道", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qq-app","QQ应用", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("cdn","cdn", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("xingyun","星云", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("itil","运营支撑", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("CDC","CDC", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qdyxb","渠道营销部", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("infosec","信息安全部", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("gongyi","腾讯公益", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqcs","营销QQ", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("hrtx","企业QQ", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qidian","企点", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("sec","安全平台部", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqpay","QQ支付", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("omd","运营管理部", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("pcmgr_bussec","电脑管家", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("smartcampus","智慧校园", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("qqsports","QQ运动", NOW(), NOW());
  INSERT INTO `devops_plugin`.`T_PLUGIN_ZHIYUN_PRODUCT` (PRODUCT_ID, PRODUCT_NAME, CREATED_TIME, UPDATED_TIME) VALUES ("retail","智慧零售", NOW(), NOW());


*/
