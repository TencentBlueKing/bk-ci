/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fastbuild

import (
	"time"

	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

const (
	mysqlTableTask           = "fastbuild_task_records"
	mysqlTableSubTask        = "fastbuild_sub_task_records"
	mysqlTableProjectSetting = "fastbuild_project_settings"
	mysqlTableProjectInfo    = "fastbuild_project_records"
	mysqlTableWhitelist      = "fastbuild_whitelist_settings"
	mysqlTableClusterSetting = "fastbuild_cluster_settings"
)

// FbSummary : fast build summary
type FbSummary struct {
	LibraryBuilt    int     `gorm:"column:library_built" json:"library_built"`
	LibraryCacheHit int     `gorm:"column:library_cache_hit" json:"library_cache_hit"`
	LibraryCPUTime  float32 `gorm:"column:library_cpu_time" json:"library_cpu_time"`

	ObjectBuilt    int     `gorm:"column:object_built" json:"object_built"`
	ObjectCacheHit int     `gorm:"column:object_cache_hit" json:"object_cache_hit"`
	ObjectCPUTime  float32 `gorm:"column:object_cpu_time" json:"object_cpu_time"`

	ExeBuilt    int     `gorm:"column:exe_built" json:"exe_built"`
	ExeCacheHit int     `gorm:"column:exe_cache_hit" json:"exe_cache_hit"`
	ExeCPUTime  float32 `gorm:"column:exe_cpu_time" json:"exe_cpu_time"`

	CacheHits   int `gorm:"column:cache_hits" json:"cache_hits"`
	CacheMisses int `gorm:"column:cache_misses" json:"cache_misses"`
	CacheStores int `gorm:"column:cache_stores" json:"cache_stores"`

	RealCompileTime   float32 `gorm:"column:real_compile_time" json:"real_compile_time"`
	LocalCompileTime  float32 `gorm:"column:local_compile_time" json:"local_compile_time"`
	RemoteCompileTime float32 `gorm:"column:remote_compile_time" json:"remote_compile_time"`
}

// TableTask define table struct of task
type TableTask struct {
	engine.TableTaskBasic

	// 资源
	ResourceID   string  `gorm:"column:resource_id" json:"resource_id"`
	RequestCPU   float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU     float64 `gorm:"column:least_cpu" json:"least_cpu"`
	CPUTotal     float64 `gorm:"column:cpu_total" json:"cpu_total"`
	MemTotal     float64 `gorm:"column:mem_total" json:"mem_total"`
	WorkerIPList string  `gorm:"column:worker_ip_list" sql:"type:text" json:"worker_ip_list"`

	// 项目属性
	CacheEnabled     bool   `gorm:"column:cache_enabled" json:"cache_enabled"`
	FBResultCompress bool   `gorm:"column:fb_result_compress" json:"fb_result_compress"`
	Attr             uint32 `gorm:"column:attr" json:"attr"`

	// 命令信息
	Params      string `gorm:"column:params" sql:"type:text" json:"params"`
	FullCmd     string `gorm:"column:full_cmd" sql:"type:text" json:"full_cmd"`
	Env         string `gorm:"column:env" sql:"type:text" json:"env"`
	RunDir      string `gorm:"column:run_dir" sql:"type:text" json:"run_dir"`
	CommandType string `gorm:"column:command_type" json:"command_type"`
	Command     string `gorm:"column:command" sql:"type:text" json:"command"`
	User        string `gorm:"column:user" sql:"type:text" json:"user"`

	// agent执行参数
	AgentMinPort            uint32 `gorm:"column:agent_min_port" json:"agent_min_port"`
	AgentMaxPort            uint32 `gorm:"column:agent_max_port" json:"agent_max_port"`
	AgentPath               string `gorm:"column:agent_path" json:"agent_path"`
	AgentRemoteExe          string `gorm:"column:agent_remote_exe" json:"agent_remote_exe"`
	AgentWorkerConsole      bool   `gorm:"column:agent_worker_console" json:"agent_worker_console"`
	AgentWorkerMode         string `gorm:"column:agent_worker_mode" json:"agent_worker_mode"`
	AgentWorkerNosubprocess bool   `gorm:"column:agent_worker_nosubprocess" json:"agent_worker_nosubprocess"`
	Agent4OneTask           bool   `gorm:"column:agent_4_one_task" json:"agent_4_one_task"`
	AgentWorkerCPU          string `gorm:"column:agent_worker_cpu" json:"agent_worker_cpu"`

	// 拉起的远端资源
	RemoteResource string `gorm:"column:remote_resource" sql:"type:text" json:"remote_resource"`

	// 结果
	CompileResult string `gorm:"column:compile_result" sql:"type:text" json:"compile_result"`
	FbSummary
}

// TableName return table name
func (tt TableTask) TableName() string {
	return mysqlTableTask
}

// TableSubTask define table struct of sub task
type TableSubTask struct {
	UpdatedAt time.Time `gorm:"column:update_at" json:"-"`

	TaskID string `gorm:"column:task_id;index" json:"task_id"`

	// 命令信息
	Params      string `gorm:"column:params" sql:"type:text" json:"params"`
	FullCmd     string `gorm:"column:full_cmd" sql:"type:text" json:"full_cmd"`
	Env         string `gorm:"column:env" sql:"type:text" json:"env"`
	RunDir      string `gorm:"column:run_dir" sql:"type:text" json:"run_dir"`
	CommandType string `gorm:"column:command_type" json:"command_type"`
	Command     string `gorm:"column:command" sql:"type:text" json:"command"`
	User        string `gorm:"column:user" sql:"type:text" json:"user"`

	// 任务状态
	Status    string `gorm:"column:status;index" json:"status"`
	StartTime int64  `gorm:"column:start_time" json:"start_time"`
	EndTime   int64  `gorm:"column:end_time" json:"end_time"`

	// 任务结果
	CompileResult string `gorm:"column:compile_result" sql:"type:text" json:"compile_result"`
	FbSummary
}

// TableName return table name
func (tst TableSubTask) TableName() string {
	return mysqlTableSubTask
}

// TableProjectSetting define table struct of project setting
type TableProjectSetting struct {
	engine.TableProjectBasic

	RequestCPU float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU   float64 `gorm:"column:least_cpu" json:"least_cpu"`

	CacheEnabled     bool   `gorm:"column:cache_enabled" json:"cache_enabled"`
	FBResultCompress bool   `gorm:"column:fb_result_compress" json:"fb_result_compress"`
	Attr             uint32 `gorm:"column:attr" json:"attr"`

	// agent worker parameters
	AgentMinPort            uint32 `gorm:"column:agent_min_port" json:"agent_min_port"`
	AgentMaxPort            uint32 `gorm:"column:agent_max_port" json:"agent_max_port"`
	AgentRemoteExe          string `gorm:"column:agent_remote_exe" json:"agent_remote_exe"`
	AgentWorkerConsole      bool   `gorm:"column:agent_worker_console" json:"agent_worker_console"`
	AgentWorkerMode         string `gorm:"column:agent_worker_mode" json:"agent_worker_mode"`
	AgentWorkerNosubprocess bool   `gorm:"column:agent_worker_nosubprocess" json:"agent_worker_nosubprocess"`
	Agent4OneTask           bool   `gorm:"column:agent_4_one_task" json:"agent_4_one_task"`
	AgentWorkerCPU          string `gorm:"column:agent_worker_cpu" json:"agent_worker_cpu"`
}

// TableName return table name
func (tps TableProjectSetting) TableName() string {
	return mysqlTableProjectSetting
}

// TableProjectInfo define table struct of project info
type TableProjectInfo struct {
	engine.TableProjectInfoBasic

	ServiceUnits float64 `gorm:"column:service_units" json:"service_units"`
}

// TableName return table name
func (tpi TableProjectInfo) TableName() string {
	return mysqlTableProjectInfo
}

// TableWhitelist define table struct of white list
type TableWhitelist struct {
	engine.TableWhitelistBasic
}

// TableName return table name
func (twl TableWhitelist) TableName() string {
	return mysqlTableWhitelist
}

// TableClusterSetting define table struct of cluster setting
type TableClusterSetting struct {
	engine.TableBasic

	Cluster     string `gorm:"column:cluster;primary_key" sql:"type:varchar(64)" json:"cluster"`
	Attr        int32  `gorm:"column:attr;default:1" json:"attr"`
	Message     string `gorm:"column:message" sql:"type:text" json:"message"`
	MessageMore string `gorm:"column:message_more" sql:"type:text" json:"message_more"`
}

// TableName return table name
func (tcs TableClusterSetting) TableName() string {
	return mysqlTableClusterSetting
}
