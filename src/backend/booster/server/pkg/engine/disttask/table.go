/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package disttask

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

const (
	mysqlTableTask            = "task_records"
	mysqlTableProjectSettings = "project_settings"
	mysqlTableProjectInfo     = "project_records"
	mysqlTableWhitelist       = "whitelist_settings"
	mysqlTableWorker          = "worker_settings"
	mysqlTableWorkStats       = "work_stats"
)

// TableTask describe the db columns of Task.
// It will inherit the TaskBasic.
type TableTask struct {
	engine.TableTaskBasic

	// client
	SourceIP           string `gorm:"column:source_ip" json:"source_ip"`
	SourceCPU          int    `gorm:"column:source_cpu" json:"source_cpu"`
	User               string `gorm:"column:user" json:"user"`
	Params             string `gorm:"column:params" sql:"type:text" json:"params"`
	Cmd                string `gorm:"column:cmd" sql:"type:text" json:"cmd"`
	Env                string `gorm:"column:env" sql:"type:text" json:"env"`
	RunDir             string `gorm:"column:run_dir" sql:"type:text" json:"run_dir"`
	BoosterType        string `gorm:"column:booster_type" json:"booster_type"`
	BanAllBooster      bool   `gorm:"column:ban_all_booster;default:false" json:"ban_all_booster"`
	ExtraClientSetting string `gorm:"column:extra_client_setting" sql:"type:text" json:"extra_client_setting"`

	// workers
	Workers string `gorm:"column:workers" sql:"type:text" json:"workers"`

	// stats
	WorkerCount int     `gorm:"column:worker_count" json:"worker_count"`
	CPUTotal    float64 `gorm:"column:cpu_total" json:"cpu_total"`
	MemTotal    float64 `gorm:"column:mem_total" json:"mem_total"`
	SucceedNum  int64   `gorm:"column:succeed_num" json:"succeed_num"`
	FailedNum   int64   `gorm:"column:failed_num" json:"failed_num"`
	StatDetail  string  `gorm:"column:stat_detail" json:"stat_detail"`
	ExtraRecord string  `gorm:"column:extra_record" sql:"type:text" json:"extra_record"`

	// resource manager
	ClusterID             string  `gorm:"column:cluster_id" json:"cluster_id"`
	AppName               string  `gorm:"column:app_name" json:"app_name"`
	Namespace             string  `gorm:"column:namespace" json:"namespace"`
	Image                 string  `gorm:"column:image" sql:"type:text" json:"image"`
	Instance              int     `gorm:"column:instance" json:"instance"`
	RequestInstance       int     `gorm:"column:request_instance" json:"request_instance"`
	LeastInstance         int     `gorm:"column:least_instance" json:"least_instance"`
	RequestCPUPerUnit     float64 `gorm:"column:request_cpu_per_unit" json:"request_cpu_per_unit"`
	RequestMemPerUnit     float64 `gorm:"column:request_mem_per_unit" json:"request_mem_per_unit"`
	RequestProcessPerUnit int     `gorm:"column:request_process_per_unit" json:"request_process_per_unit"`

	// inherit from project setting or worker setting
	RequestCPU          float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU            float64 `gorm:"column:least_cpu" json:"least_cpu"`
	WorkerVersion       string  `gorm:"column:worker_version" json:"worker_version"`
	Scene               string  `gorm:"column:scene" json:"scene"`
	ExtraProjectSetting string  `gorm:"column:extra_project_setting" sql:"type:text" json:"extra_project_setting"`
	ExtraWorkerSetting  string  `gorm:"column:extra_worker_setting" sql:"type:text" json:"extra_worker_setting"`
}

// TableName specific table name.
func (tt TableTask) TableName() string {
	return mysqlTableTask
}

// TableProjectSetting describe the db columns of project setting.
// It will inherit the ProjectBasic.
type TableProjectSetting struct {
	engine.TableProjectBasic

	RequestCPU float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU   float64 `gorm:"column:least_cpu" json:"least_cpu"`
	SuggestCPU float64 `gorm:"column:suggest_cpu;default:0" json:"suggest_cpu"`

	WorkerVersion string `gorm:"column:worker_version" json:"worker_version"`
	Scene         string `gorm:"column:scene" json:"scene"`
	BanAllBooster bool   `gorm:"column:ban_all_booster;default:false" json:"ban_all_booster"`

	// reserved for different project
	Extra string `gorm:"column:extra" json:"extra"`
}

// TableName specific table name.
func (tps TableProjectSetting) TableName() string {
	return mysqlTableProjectSettings
}

// TableProjectInfo describe the db columns of project info.
// It will inherit the ProjectInfoBasic.
type TableProjectInfo struct {
	engine.TableProjectInfoBasic

	CompileFilesOK      int64   `gorm:"column:compile_files_ok" json:"compile_files_ok"`
	CompileFilesErr     int64   `gorm:"column:compile_files_err" json:"compile_files_err"`
	CompileFilesTimeout int64   `gorm:"column:compile_files_timeout" json:"compile_files_timeout"`
	CompileUnits        float64 `gorm:"column:compile_units" json:"compile_units"`
}

// TableName specific table name.
func (tpi TableProjectInfo) TableName() string {
	return mysqlTableProjectInfo
}

// TableWhitelist describe the db columns of whitelist.
// It will inherit the WhitelistBasic.
type TableWhitelist struct {
	engine.TableWhitelistBasic
}

// TableName specific table name.
func (twl TableWhitelist) TableName() string {
	return mysqlTableWhitelist
}

// TableWorker describe the db columns of worker.
// It inherit directly from Basic.
type TableWorker struct {
	engine.TableBasic

	WorkerVersion string `gorm:"column:worker_version;primary_key" json:"worker_version"`
	Scene         string `gorm:"column:scene;primary_key" json:"scene"`
	Image         string `gorm:"column:image" sql:"type:text" json:"image"`

	// reserved
	Extra string `gorm:"column:extra" json:"extra"`
}

// TableName specific table name.
func (tg TableWorker) TableName() string {
	return mysqlTableWorker
}

// CheckData check if worker data valid.
func (tg *TableWorker) CheckData() error {
	if tg.WorkerVersion == "" {
		return fmt.Errorf("worker version empty")
	}
	if tg.Scene == "" {
		return fmt.Errorf("worker scene empty")
	}
	if tg.Image == "" {
		return fmt.Errorf("image empty")
	}
	return nil
}

// TableWorkStats
type TableWorkStats struct {
	engine.TableBasic

	ID               int    `gorm:"column:id;AUTO_INCREMENT" json:"id"`
	ProjectID        string `gorm:"column:project_id" json:"project_id"`
	TaskID           string `gorm:"column:task_id;index" json:"task_id"`
	WorkID           string `gorm:"column:work_id;index" json:"work_id"`
	Scene            string `gorm:"column:scene;index" json:"scene"`
	Success          bool   `gorm:"column:success;index" json:"success"`
	JobRemoteOK      int    `gorm:"column:job_remote_ok" json:"job_remote_ok"`
	JobRemoteError   int    `gorm:"column:job_remote_error" json:"job_remote_error"`
	JobLocalOK       int    `gorm:"column:job_local_ok" json:"job_local_ok"`
	JobLocalError    int    `gorm:"column:job_local_error" json:"job_local_error"`
	StartTime        int64  `gorm:"column:start_time" json:"start_time"`
	EndTime          int64  `gorm:"column:end_time" json:"end_time"`
	RegisteredTime   int64  `gorm:"column:registered_time" json:"registered_time"`
	UnregisteredTime int64  `gorm:"column:unregistered_time" json:"unregistered_time"`
	JobStats         string `gorm:"column:job_stats;type:longtext" json:"job_stats"`
}

// TableName specific table name.
func (tts *TableWorkStats) TableName() string {
	return mysqlTableWorkStats
}
