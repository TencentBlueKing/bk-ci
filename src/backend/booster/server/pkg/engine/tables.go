/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package engine

import (
	"fmt"
	"reflect"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"

	// 启用mysql driver
	_ "github.com/jinzhu/gorm/dialects/mysql"
)

// TableBasic contains some basic fields in all tables.
type TableBasic struct {
	UpdatedAt time.Time `gorm:"column:update_at" json:"-"`
	Disabled  bool      `gorm:"column:disabled;default:false;index" json:"-"`
}

// TableTaskBasic contains the task basic fields. All engine task should contains these basic fields.
type TableTaskBasic struct {
	TableBasic
	TaskID string `gorm:"column:task_id;primary_key" json:"task_id"`

	// basic client
	EngineName    string `gorm:"column:engine_name" json:"engine_name"`
	QueueName     string `gorm:"column:queue_name" json:"queue_name"`
	ProjectID     string `gorm:"column:project_id;index" json:"project_id"`
	BuildID       string `gorm:"column:build_id" json:"build_id"`
	ClientIP      string `gorm:"column:client_ip" sql:"type:text" json:"client_ip"`
	ClientCPU     int    `gorm:"column:client_cpu" json:"client_cpu"`
	ClientVersion string `gorm:"column:client_version" json:"client_version"`
	ClientMessage string `gorm:"column:client_message" sql:"type:text" json:"client_message"`
	StageTimeout  int    `gorm:"column:stage_timeout;default:60" json:"stage_timeout"`
	Priority      int    `gorm:"column:priority" json:"priority"`

	// basic status
	Status            string `gorm:"column:status;index" json:"status"`
	StatusCode        int    `gorm:"column:status_code" json:"status_code"`
	StatusMessage     string `gorm:"column:status_message" sql:"type:text" json:"status_message"`
	Released          bool   `gorm:"column:released;index" json:"released"`
	LastHeartBeatTime int64  `gorm:"column:last_heartbeat_time" json:"last_heartbeat_time"`
	StatusChangeTime  int64  `gorm:"column:status_change_time" json:"status_change_time"`
	InitTime          int64  `gorm:"column:init_time" json:"init_time"`
	CreateTime        int64  `gorm:"column:create_time;index" json:"create_time"`
	UpdateTime        int64  `gorm:"column:update_time" json:"update_time"`
	LaunchTime        int64  `gorm:"column:launch_time" json:"launch_time"`
	ReadyTime         int64  `gorm:"column:ready_time" json:"ready_time"`
	ShutDownTime      int64  `gorm:"column:shutdown_time" json:"shutdown_time"`
	StartTime         int64  `gorm:"column:start_time" json:"start_time"`
	EndTime           int64  `gorm:"column:end_time" json:"end_time"`
}

// TableProjectBasic contains the project basic fields. All engine project should contains these basic fields.
type TableProjectBasic struct {
	TableBasic

	ProjectID    string `gorm:"column:project_id;primary_key" json:"project_id"`
	ProjectName  string `gorm:"column:project_name" json:"project_name"`
	Priority     int    `gorm:"column:priority" json:"priority"`
	EngineName   string `gorm:"column:engine_name" json:"engine_name"`
	QueueName    string `gorm:"column:queue_name" json:"queue_name"`
	StageTimeout int    `gorm:"column:stage_timeout;default:60" json:"stage_timeout"`
	Message      string `gorm:"column:message" sql:"type:text" json:"message"`
	Concurrency  int    `gorm:"column:concurrency;default:0" json:"concurrency"`
}

// TableProjectInfoBasic contains the project info basic fields.
type TableProjectInfoBasic struct {
	TableBasic

	ProjectID          string `gorm:"column:project_id;primary_key" json:"project_id"`
	CompileFinishTimes int64  `gorm:"column:compile_finish_times" json:"compile_finish_times"`
	CompileFailedTimes int64  `gorm:"column:compile_failed_times" json:"compile_failed_times"`
}

// DeltaProjectInfoBasic contains the delta part of project info basic.
type DeltaProjectInfoBasic struct {
	CompileFinishTimes int64
	CompileFailedTimes int64
}

// TableProjectBasic contains the project basic fields. All engine project should contains these basic fields.
type TableWhitelistBasic struct {
	TableBasic

	IP        string `gorm:"column:ip;primary_key" json:"ip"`
	ProjectID string `gorm:"column:project_id;primary_key" json:"project_id"`
	Message   string `gorm:"column:message" sql:"type:text" json:"message"`
}

// CheckData check whitelist basic data
func (tb *TableWhitelistBasic) CheckData() error {
	if tb.IP == "" {
		return ErrorIPNotSpecified
	}

	// if project is empty, regard it as a all project whitelist.
	if tb.ProjectID == "" {
		tb.ProjectID = WhiteListAllProjectID
	}
	return nil
}

const (
	WhiteListAllProjectID = "DIST_CC_CONST::ALL_PROJECT"
)

// WhiteListKey describe the primary key in whitelist basic, it is a ip - projectID pair.
type WhiteListKey struct {
	IP        string `json:"ip"`
	ProjectID string `json:"project_id"`
}

// MySQLConf describe the mysql connection config need by engines
type MySQLConf struct {
	MySQLStorage     string
	MySQLDatabase    string
	MySQLUser        string
	MySQLPwd         string
	Charset          string
	MySQLDebug       bool
	MysqlTableOption string
}

// CheckTaskIDValid check if the taskID is valid, no used. By checking the engine task basic.
func CheckTaskIDValid(egn Engine, taskID string) (bool, error) {
	defer timeMetricRecord(egn, "check_task_id_valid")()

	var tbl []*TableTaskBasic

	opts := commonMySQL.NewListOptions()
	opts.Limit(-1)
	opts.Equal("task_id", taskID)
	db := opts.AddWhere(egn.GetTaskBasicTable()).Find(&tbl)

	if err := db.Error; err != nil {
		return false, err
	}

	if len(tbl) < 1 {
		return true, nil
	}

	return false, nil
}

// ListTaskBasic list task basic from engine
func ListTaskBasic(egn Engine, options TaskListOptions) ([]*TaskBasic, error) {
	defer timeMetricRecord(egn, "list_task_basic")()

	var tbl []*TableTaskBasic

	opts := commonMySQL.NewListOptions()
	opts.Limit(-1)
	if len(options.Status) > 0 {
		opts.In("status", options.Status)
	}
	if options.Released != nil {
		opts.Equal("released", *options.Released)
	}
	// var ll []map[string]interface{}
	db := opts.AddWhere(egn.GetTaskBasicTable()).Where("disabled = ?", false).Find(&tbl)

	if err := db.Error; err != nil {
		return nil, err
	}

	r := make([]*TaskBasic, 0, 100)
	for _, t := range tbl {
		r = append(r, TableTask2TaskBasic(t))
	}

	return r, nil
}

// CreateTaskBasic create new task basic into database
func CreateTaskBasic(egn Engine, tb *TaskBasic) error {
	defer timeMetricRecord(egn, "create_task_basic")()

	return egn.GetTaskBasicTable().Create(TaskBasic2TableTask(tb)).Error
}

// UpdateTaskBasic update task basic into database
func UpdateTaskBasic(egn Engine, tb *TaskBasic) error {
	defer timeMetricRecord(egn, "update_task_basic")()

	var tmp []byte
	_ = codec.EncJSON(TaskBasic2TableTask(tb), &tmp)
	var data map[string]interface{}
	_ = codec.DecJSON(tmp, &data)
	delete(data, "task_id")
	return egn.GetTaskBasicTable().Where("task_id = ?", tb.ID).Updates(data).Error
}

// UpdateProjectInfoBasic update the project info basic with delta data.
func UpdateProjectInfoBasic(egn Engine, projectID string, delta DeltaProjectInfoBasic) error {
	defer timeMetricRecord(egn, "update_project_info_basic")()

	tx := egn.GetProjectInfoBasicTable().Begin()

	var pi TableProjectInfoBasic
	pi.ProjectID = projectID
	if err := tx.Set("gorm:query_option", "FOR UPDATE").FirstOrCreate(&pi).Error; err != nil {
		tx.Rollback()
		return err
	}

	pi.CompileFinishTimes += delta.CompileFinishTimes
	pi.CompileFailedTimes += delta.CompileFailedTimes

	if err := tx.Save(&pi).Error; err != nil {
		tx.Rollback()
		return err
	}

	tx.Commit()
	return nil
}

// GetProjectBasic get project basic from engine
func GetProjectBasic(egn Engine, projectID string) (*ProjectBasic, error) {
	defer timeMetricRecord(egn, "get_project_basic")()

	var psl []*TableProjectBasic
	db := egn.GetProjectBasicTable().Where("project_id = ?", projectID).
		Where("disabled = ?", false).Find(&psl)

	if err := db.Error; err != nil {
		return nil, err
	}

	if len(psl) < 1 {
		return nil, ErrorProjectNoFound
	}

	return TableProject2ProjectBasic(psl[0]), nil
}

// ListWhitelistBasic list whitelist basic from engine
func ListWhitelistBasic(egn Engine, projectID string) ([]*WhitelistBasic, error) {
	defer timeMetricRecord(egn, "list_whitelist_basic")()

	var wll []*TableWhitelistBasic
	db := egn.GetWhitelistBasicTable().Where("(project_id = ? or project_id = ?) and disabled = ?",
		projectID, WhiteListAllProjectID, false).Find(&wll)

	if err := db.Error; err != nil {
		return nil, err
	}

	if len(wll) < 1 {
		return nil, ErrorWhitelistNoFound
	}

	r := make([]*WhitelistBasic, 0, 100)
	for _, wl := range wll {
		r = append(r, TableWhitelist2WhitelistBasic(wl))
	}

	return r, nil
}

// TableTask2TaskBasic convert TaskBasic from database field into struct data
func TableTask2TaskBasic(table *TableTaskBasic) *TaskBasic {
	return &TaskBasic{
		ID: table.TaskID,
		Client: TaskBasicClient{
			EngineName:    TypeName(table.EngineName),
			QueueName:     table.QueueName,
			ProjectID:     table.ProjectID,
			BuildID:       table.BuildID,
			ClientIP:      table.ClientIP,
			ClientCPU:     table.ClientCPU,
			ClientVersion: table.ClientVersion,
			Message:       table.ClientMessage,
			StageTimeout:  table.StageTimeout,
			Priority:      TaskPriority(table.Priority),
		},
		Status: TaskBasicStatus{
			Status:     TaskStatusType(table.Status),
			StatusCode: TaskStatusCode(table.StatusCode),
			Message:    table.StatusMessage,

			Released:          table.Released,
			LastHeartBeatTime: time.Unix(table.LastHeartBeatTime, 0),
			StatusChangeTime:  time.Unix(table.StatusChangeTime, 0),
			InitTime:          time.Unix(table.InitTime, 0),
			CreateTime:        time.Unix(table.CreateTime, 0),
			UpdateTime:        time.Unix(table.UpdateTime, 0),
			LaunchTime:        time.Unix(table.LaunchTime, 0),
			ReadyTime:         time.Unix(table.ReadyTime, 0),
			ShutDownTime:      time.Unix(table.ShutDownTime, 0),
			StartTime:         time.Unix(table.StartTime, 0),
			EndTime:           time.Unix(table.EndTime, 0),
		},
	}
}

// TaskBasic2TableTask convert TaskBasic from struct data into database field
func TaskBasic2TableTask(basic *TaskBasic) *TableTaskBasic {
	return &TableTaskBasic{
		TaskID: basic.ID,
		// basic client
		EngineName:    basic.Client.EngineName.String(),
		QueueName:     basic.Client.QueueName,
		ProjectID:     basic.Client.ProjectID,
		BuildID:       basic.Client.BuildID,
		ClientIP:      basic.Client.ClientIP,
		ClientCPU:     basic.Client.ClientCPU,
		ClientVersion: basic.Client.ClientVersion,
		ClientMessage: basic.Client.Message,
		StageTimeout:  basic.Client.StageTimeout,
		Priority:      int(basic.Client.Priority),

		// basic status
		Status:            string(basic.Status.Status),
		StatusCode:        int(basic.Status.StatusCode),
		StatusMessage:     basic.Status.Message,
		Released:          basic.Status.Released,
		LastHeartBeatTime: basic.Status.LastHeartBeatTime.Unix(),
		StatusChangeTime:  basic.Status.StatusChangeTime.Unix(),
		InitTime:          basic.Status.InitTime.Unix(),
		CreateTime:        basic.Status.CreateTime.Unix(),
		UpdateTime:        basic.Status.UpdateTime.Unix(),
		LaunchTime:        basic.Status.LaunchTime.Unix(),
		ReadyTime:         basic.Status.ReadyTime.Unix(),
		ShutDownTime:      basic.Status.ShutDownTime.Unix(),
		StartTime:         basic.Status.StartTime.Unix(),
		EndTime:           basic.Status.EndTime.Unix(),
	}
}

// TableProject2ProjectBasic convert ProjectBasic from database field into struct data
func TableProject2ProjectBasic(basic *TableProjectBasic) *ProjectBasic {
	return &ProjectBasic{
		ProjectID:    basic.ProjectID,
		ProjectName:  basic.ProjectName,
		Priority:     TaskPriority(basic.Priority),
		EngineName:   TypeName(basic.EngineName),
		QueueName:    basic.QueueName,
		StageTimeout: basic.StageTimeout,
		Message:      basic.Message,
		Concurrency:  basic.Concurrency,
	}
}

// TableWhitelist2WhitelistBasic convert WhitelistBasic from database field into struct data
func TableWhitelist2WhitelistBasic(basic *TableWhitelistBasic) *WhitelistBasic {
	return &WhitelistBasic{
		ProjectID: basic.ProjectID,
		IP:        basic.IP,
		Message:   basic.Message,
	}
}

// GetMapExcludeTableTaskBasic received a table which inherit the TableTaskBasic
// and marshal it into map[string]interface{}, then remove the keys provided by TableTaskBasic
// Then the return data can be used to update table and will never change TableTaskBasic
func GetMapExcludeTableTaskBasic(source interface{}) (map[string]interface{}, error) {
	return getMapExcludeBasicTable(TableTaskBasic{}, source)
}

func getMapExcludeBasicTable(table, source interface{}) (map[string]interface{}, error) {
	var tmp []byte
	if err := codec.EncJSON(source, &tmp); err != nil {
		return nil, err
	}

	var data map[string]interface{}
	if err := codec.DecJSON(tmp, &data); err != nil {
		return nil, err
	}

	t := reflect.TypeOf(table)
	if t.Kind() == reflect.Ptr {
		t = t.Elem()
	}
	n := t.NumField()
	for i := 0; i < n; i++ {
		key, ok := t.Field(i).Tag.Lookup("json")
		if !ok || key == "" {
			continue
		}
		delete(data, key)
	}

	return data, nil
}

func timeMetricRecord(egn Engine, operation string) func() {
	return selfMetric.TimeMetricRecord(fmt.Sprintf("%s_%s", egn.Name(), operation))
}
