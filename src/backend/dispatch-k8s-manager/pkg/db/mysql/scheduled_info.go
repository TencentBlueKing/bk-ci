package mysql

import (
	"database/sql"
	"disaptch-k8s-manager/pkg/types"
	"encoding/json"
	"github.com/pkg/errors"
)

const insertOrUpdateScheduledInfoSql = "INSERT INTO T_KUBERNETES_MANAGER_BUILDER_SCHEDULED_INFO(`BUILDER_NAME`,`NODE_HISTORY`,`RESOURCE_HISTORY`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE NODE_HISTORY=?, RESOURCE_HISTORY=?"

func InsertOrUpdateScheduledInfo(info types.ScheduledInfo) error {
	stmt, err := Mysql.Prepare(insertOrUpdateScheduledInfoSql)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	nodeHisJ, err := json.Marshal(info.NodeHistory)
	if err != nil {
		return errors.Wrap(err, "json marshal nodeHis error")
	}
	resHisJ, err := json.Marshal(info.ResourceHistory)
	if err != nil {
		return errors.Wrap(err, "json marshal resHisJ error")
	}

	if _, err := stmt.Exec(
		info.BuilderName,
		nodeHisJ,
		resHisJ,
		nodeHisJ,
		resHisJ,
	); err != nil {
		return sqlError(err)
	}
	return nil
}

const selectScheduledInfoSql = "SELECT NODE_HISTORY,RESOURCE_HISTORY FROM T_KUBERNETES_MANAGER_BUILDER_SCHEDULED_INFO WHERE BUILDER_NAME = ?"

func SelectScheduledInfo(builderName string) (*types.ScheduledInfo, error) {
	var nodeHisJ []byte
	var resHisJ []byte

	err := Mysql.QueryRow(selectScheduledInfoSql, builderName).Scan(&nodeHisJ, &resHisJ)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, sqlError(err)
	}

	info := new(types.ScheduledInfo)
	info.BuilderName = builderName

	if err = json.Unmarshal(nodeHisJ, &info.NodeHistory); err != nil {
		return nil, errors.Wrap(err, "json unmarshal nodeHis error")
	}
	if err = json.Unmarshal(resHisJ, &info.ResourceHistory); err != nil {
		return nil, errors.Wrap(err, "json unmarshal resHis error")
	}

	return info, nil
}

const deleteScheduledInfoByName = "DELETE FROM T_KUBERNETES_MANAGER_BUILDER_SCHEDULED_INFO WHERE BUILDER_NAME = ?"

func DeleteScheduledInfoByName(builderName string) error {
	stmt, err := Mysql.Prepare(deleteScheduledInfoByName)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(builderName); err != nil {
		return sqlError(err)
	}
	return nil
}
