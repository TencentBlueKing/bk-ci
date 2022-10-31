package mysql

import (
	"database/sql"
	"disaptch-k8s-manager/pkg/types"
)

const insertTaskSql = "INSERT INTO T_KUBERNETES_MANAGER_TASK(`TASK_ID`,`TASK_KEY`,`TASK_BELONG`,`ACTION`,`STATUS`,`MESSAGE`) VALUES (?,?,?,?,?,?)"

func InsertTask(task types.Task) error {
	stmt, err := Mysql.Prepare(insertTaskSql)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(
		task.TaskId,
		task.TaskKey,
		task.TaskBelong,
		task.Action,
		task.Status,
		task.Message,
	); err != nil {
		return sqlError(err)
	}
	return nil
}

const updateTaskSql = "UPDATE T_KUBERNETES_MANAGER_TASK SET STATUS = ?, MESSAGE = ? WHERE TASK_ID = ?"

func UpdateTask(taskId string, status types.TaskState, message string) error {
	stmt, err := Mysql.Prepare(updateTaskSql)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(
		status,
		message,
		taskId,
	); err != nil {
		return sqlError(err)
	}
	return nil
}

const selectTaskSql = "SELECT STATUS,MESSAGE FROM T_KUBERNETES_MANAGER_TASK WHERE TASK_ID = ?"

func SelectTaskStatus(taskId string) (*types.TaskStatus, error) {
	task := &types.TaskStatus{
		Status:  new(types.TaskState),
		Message: []byte{},
	}

	err := Mysql.QueryRow(selectTaskSql, taskId).Scan(task.Status, &task.Message)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, sqlError(err)
	}

	return task, nil
}

const deleteTaskByUpdateTime = "DELETE FROM T_KUBERNETES_MANAGER_TASK WHERE UPDATE_TIME < DATE_SUB(NOW(), INTERVAL ? DAY);"

func DeleteTaskByUpdateTime(day int) error {
	stmt, err := Mysql.Prepare(deleteTaskByUpdateTime)
	if err != nil {
		return sqlError(err)
	}
	defer stmt.Close()

	if _, err := stmt.Exec(day); err != nil {
		return sqlError(err)
	}
	return nil
}
