package mysql

import (
	"database/sql"
	"disaptch-k8s-manager/pkg/types"
)

const insertBuildLessTaskSql = "INSERT INTO T_KUBERNETES_BUILDLESS_HISTORY(`POD_ID`,`PROJECT_ID`,`PIPELINE_ID`,`BUILD_ID`,`VM_SEQ_ID`,`EXECUTION_COUNT`,`STATUS`) VALUES (?,?,?,?,?,?,?)"

func InsertBuildLessTask(podId string, task types.BuildLessTask) error {
	stmt, err := Mysql.Prepare(insertBuildLessTaskSql)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(
		podId,
		task.ProjectId,
		task.PipelineId,
		task.BuildId,
		task.VmSeqId,
		task.ExecutionCount,
		types.BuildLessTaskRunning,
	); err != nil {
		return sqlError(err)
	}
	return nil
}

const updateBuildLessTaskSql = "UPDATE T_KUBERNETES_BUILDLESS_HISTORY SET STATUS = ? WHERE BUILD_ID = ? AND VM_SEQ_ID = ? AND EXECUTION_COUNT = ?"

func UpdateBuildLessTask(buildId string, vmSeqId string, executionCount string, status types.BuildLessTaskState) error {
	stmt, err := Mysql.Prepare(updateBuildLessTaskSql)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(
		status,
		buildId,
		vmSeqId,
		executionCount,
	); err != nil {
		return sqlError(err)
	}
	return nil
}

const selectBuildLessTaskSql = "SELECT PROJECT_ID, PIPELINE_ID, BUILD_ID, VM_SEQ_ID, EXECUTION_COUNT, STATUS FROM T_KUBERNETES_BUILDLESS_HISTORY WHERE POD_ID = ?"

func SelectBuildLessTask(podId string) (*types.BuildLessTask, error) {
	var status = new(types.BuildLessTaskState)
	var buildLessTask = &types.BuildLessTask{}
	stmt, err := Mysql.Prepare(selectBuildLessTaskSql)
	if err != nil {
		return nil, sqlError(err)
	}
	defer stmt.Close()

	err = stmt.QueryRow(podId).Scan(&buildLessTask.ProjectId, &buildLessTask.PipelineId, &buildLessTask.BuildId, &buildLessTask.VmSeqId, &buildLessTask.ExecutionCount, &status)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, sqlError(err)
	}

	return buildLessTask, nil
}
