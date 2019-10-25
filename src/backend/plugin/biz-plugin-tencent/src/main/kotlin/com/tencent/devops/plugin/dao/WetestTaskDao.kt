package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TPluginWetestTask
import com.tencent.devops.model.plugin.tables.records.TPluginWetestTaskRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WetestTaskDao {

    fun insert(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        mobileCategory: String,
        mobileCategoryId: String,
        mobileModel: String,
        mobileModelId: String,
        description: String?
    ): Int {

        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            val data = dslContext.insertInto(this,
                    PROJECT_ID,
                    NAME,
                    MOBILE_CATEGORY,
                    MOBILE_CATEGORY_ID,
                    MOBILE_MODEL,
                    MOBILE_MODEL_ID,
                    DESCRIPTION,
                    CREATED_TIME,
                    UPDATED_TIME)
                    .values(projectId,
                            name,
                            mobileCategory,
                            mobileCategoryId,
                            mobileModel,
                            mobileModelId,
                            description,
                            LocalDateTime.now(),
                            LocalDateTime.now())
                    .returning(ID)
                    .fetchOne()
            return data.id
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        id: Int,
        name: String,
        mobileCategory: String,
        mobileCategoryId: String,
        mobileModel: String,
        mobileModelId: String,
        description: String?
    ) {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            dslContext.update(this)
                    .set(NAME, name)
                    .set(MOBILE_CATEGORY, mobileCategory)
                    .set(MOBILE_CATEGORY_ID, mobileCategoryId)
                    .set(MOBILE_MODEL, mobileModel)
                    .set(MOBILE_MODEL_ID, mobileModelId)
                    .set(DESCRIPTION, description)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(ID.eq(id)).and(PROJECT_ID.eq(projectId))
                    .execute()
        }
    }

    fun getList(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Result<TPluginWetestTaskRecord>? {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                    .orderBy(UPDATED_TIME.desc())
                    .limit(pageSize).offset((page - 1) * pageSize)
                    .fetch()
        }
    }

    fun getCount(dslContext: DSLContext, projectId: String): Int {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId))
                    .fetchOne().get(0) as Int
        }
    }

    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        id: Int
    ): TPluginWetestTaskRecord? {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id)).and(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun getByName(
        dslContext: DSLContext,
        projectId: String,
        name: String
    ): TPluginWetestTaskRecord? {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)).and(NAME.eq(name))
                    .fetchOne()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        id: Int
    ) {
        with(TPluginWetestTask.T_PLUGIN_WETEST_TASK) {
            dslContext.deleteFrom(this)
                    .where(ID.eq(id)).and(PROJECT_ID.eq(projectId))
                    .execute()
        }
    }
}

/*

DROP TABLE IF EXISTS `devops_plugin`.`T_PLUGIN_WETEST_TASK`;
CREATE TABLE `T_PLUGIN_WETEST_TASK` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PROJECT_ID` varchar(128) NOT NULL,
  `NAME` varchar(128) NOT NULL,
  `MOBILE_CATEGORY` varchar(64) NOT NULL,
  `MOBILE_CATEGORY_ID` varchar(64) NOT NULL,
  `MOBILE_MODEL` longtext NOT NULL,
  `MOBILE_MODEL_ID` longtext,
  `DESCRIPTION` varchar(1024) NULL,
  `TICKETS_ID` varchar(64) NULL,
  `CREATED_TIME` datetime NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=12522 DEFAULT CHARSET=utf8;

*/