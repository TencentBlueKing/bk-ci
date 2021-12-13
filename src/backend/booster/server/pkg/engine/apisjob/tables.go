/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package apisjob

import (
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

const (
	mysqlTableTask           = "apis_task_records"
	mysqlTableProjectSetting = "apis_project_settings"
	mysqlTableProjectInfo    = "apis_project_records"
	mysqlTableWhitelist      = "apis_whitelist_settings"
)

// TableTask describe the db columns of Task.
// It will inherit the TaskBasic.
type TableTask struct {
	engine.TableTaskBasic

	ResourceID string  `gorm:"column:resource_id" json:"resource_id"`
	RequestCPU float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU   float64 `gorm:"column:least_cpu" json:"least_cpu"`
	CPUTotal   float64 `gorm:"column:cpu_total" json:"cpu_total"`
	MemTotal   float64 `gorm:"column:mem_total" json:"mem_total"`

	// agent worker project_id, specified by client
	AgentProjectID string `gorm:"column:agent_project_id" json:"agent_project_id"`
	WorkerIPList   string `gorm:"column:worker_ip_list" sql:"type:text" json:"worker_ip_list"`

	// stats from client
	CompleteTasks int    `gorm:"column:complete_tasks" json:"complete_tasks"`
	FailedTasks   int    `gorm:"column:failed_tasks" json:"failed_tasks"`
	AgentsInfo    string `gorm:"column:agents_info" sql:"type:text" json:"agents_info"`

	// container settings
	Image           string  `gorm:"column:image" sql:"type:text" json:"image"`
	CPUPerInstance  float64 `gorm:"cpu_per_instance" json:"cpu_per_instance"`
	MemPerInstance  float64 `gorm:"mem_per_instance" json:"mem_per_instance"`
	Instance        int     `gorm:"instance" json:"instance"`
	LeastInstance   int     `gorm:"least_instance" json:"least_instance"`
	RequestInstance int     `gorm:"request_instance" json:"request_instance"`

	// set from project settings
	CoordinatorIP            string `gorm:"column:coordinator_ip" json:"coordinator_ip"`
	CoordinatorPort          int    `gorm:"column:coordinator_port" json:"coordinator_port"`
	AgentCommandName         string `gorm:"column:agent_command_name" json:"agent_command_name"`
	AgentCommandPath         string `gorm:"column:agent_command_path" sql:"type:text" json:"agent_command_path"`
	AgentCommandDir          string `gorm:"column:agent_command_dir" sql:"type:text" json:"agent_command_dir"`
	AgentPort                int    `gorm:"column:agent_port" json:"agent_port"`
	AgentFileServerPort      int    `gorm:"column:agent_file_server_port" json:"agent_file_server_port"`
	AgentNTriesConnectingRPC int    `gorm:"column:agent_n_tries_connecting_rpc" json:"agent_n_tries_connecting_rpc"`
	AgentCachePath           string `gorm:"column:agent_cache_path" sql:"type:text" json:"agent_cache_path"`
	CacheIP                  string `gorm:"column:cache_ip" json:"cache_ip"`
	CachePort                int    `gorm:"column:cache_port" json:"cache_port"`
	UseGdt                   bool   `gorm:"column:use_gdt" json:"use_gdt"`
	VolumeMounts             string `gorm:"column:volume_mounts" sql:"type:text" json:"volume_mounts"`

	// following fields all deprecated, but will be kept in database table as columns.
	AgentMaxTasksPerJob           int   `gorm:"column:agent_max_tasks_per_job" json:"agent_max_tasks_per_job"`
	AgentMaxTasks                 int   `gorm:"column:agent_max_tasks" json:"agent_max_tasks"`
	AgentKeepAliveInterval        int   `gorm:"column:agent_keep_alive_interval" json:"agent_keep_alive_interval"`
	AgentDependPreparationTimeout int   `gorm:"column:agent_depend_preparation_timeout" json:"agent_depend_preparation_timeout"`
	AgentClientKeepAliveTimeout   int   `gorm:"column:agent_client_keep_alive_timeout" json:"agent_client_keep_alive_timeout"`
	AgentFileRetrievingRetryTimes int   `gorm:"column:agent_file_retrieving_retry_times" json:"agent_file_retrieving_retry_times"`
	AgentFileRequestTimeout       int   `gorm:"column:agent_file_request_timeout" json:"agent_file_request_timeout"`
	AgentMaxWorkingProcesses      int   `gorm:"column:agent_max_working_processes" json:"agent_max_working_processes"`
	AgentMaxWorkingCapability     int   `gorm:"column:agent_max_working_capability" json:"agent_max_working_capability"`
	AgentMaxCores                 int   `gorm:"column:agent_max_cores" json:"agent_max_cores"`
	AgentMaxFileRetrievingCount   int   `gorm:"column:agent_max_file_retrieving_count" json:"agent_max_file_retrieving_count"`
	AgentNoCoordinator            bool  `gorm:"column:agent_no_coordinator;default:true" json:"agent_no_coordinator"`
	AgentFilePackageSize          int64 `gorm:"column:agent_file_package_size" json:"agent_file_package_size"`
	AgentChunkRetryInterval       int   `gorm:"column:agent_chunk_retry_interval" json:"agent_chunk_retry_interval"`
	AgentMaxTransmissionPeers     int   `gorm:"column:agent_max_transmission_peers" json:"agent_max_transmission_peers"`
	AgentMaxActiveTransmission    int   `gorm:"column:agent_max_active_transmission" json:"agent_max_active_transmission"`
	AgentEnableFileCompress       bool  `gorm:"column:agent_enable_file_compress" json:"agent_enable_file_compress"`
	AgentPrepareTimeout           int   `gorm:"column:agent_prepare_timeout" json:"agent_prepare_timeout"`
	AgentEnableFileLog            bool  `gorm:"column:agent_enable_file_log" json:"agent_enable_file_log"`
}

// specific table name.
func (tt TableTask) TableName() string {
	return mysqlTableTask
}

// TableProjectSetting describe the db columns of project setting.
// It will inherit the ProjectBasic.
type TableProjectSetting struct {
	engine.TableProjectBasic

	RequestCPU float64 `gorm:"column:request_cpu" json:"request_cpu"`
	LeastCPU   float64 `gorm:"column:least_cpu" json:"least_cpu"`

	// agent worker parameters
	CoordinatorIP            string `gorm:"column:coordinator_ip" json:"coordinator_ip"`
	CoordinatorPort          int    `gorm:"column:coordinator_port" json:"coordinator_port"`
	AgentCommandName         string `gorm:"column:agent_command_name" json:"agent_command_name"`
	AgentCommandPath         string `gorm:"column:agent_command_path" sql:"type:text" json:"agent_command_path"`
	AgentCommandDir          string `gorm:"column:agent_command_dir" sql:"type:text" json:"agent_command_dir"`
	AgentPort                int    `gorm:"column:agent_port" json:"agent_port"`
	AgentFileServerPort      int    `gorm:"column:agent_file_server_port" json:"agent_file_server_port"`
	AgentNTriesConnectingRPC int    `gorm:"column:agent_n_tries_connecting_rpc" json:"agent_n_tries_connecting_rpc"`
	AgentCachePath           string `gorm:"column:agent_cache_path" sql:"type:text" json:"agent_cache_path"`
	Image                    string `gorm:"column:image" sql:"type:text" json:"image"`
	CacheIP                  string `gorm:"column:cache_ip" json:"cache_ip"`
	CachePort                int    `gorm:"column:cache_port" json:"cache_port"`
	UseGdt                   bool   `gorm:"column:use_gdt" json:"use_gdt"`
	VolumeMounts             string `gorm:"column:volume_mounts" sql:"type:text" json:"volume_mounts"`

	// following fields all deprecated, but will be kept in database table as columns.
	AgentMaxTasksPerJob           int   `gorm:"column:agent_max_tasks_per_job" json:"agent_max_tasks_per_job"`
	AgentMaxTasks                 int   `gorm:"column:agent_max_tasks" json:"agent_max_tasks"`
	AgentKeepAliveInterval        int   `gorm:"column:agent_keep_alive_interval" json:"agent_keep_alive_interval"`
	AgentDependPreparationTimeout int   `gorm:"column:agent_depend_preparation_timeout" json:"agent_depend_preparation_timeout"`
	AgentClientKeepAliveTimeout   int   `gorm:"column:agent_client_keep_alive_timeout" json:"agent_client_keep_alive_timeout"`
	AgentFileRetrievingRetryTimes int   `gorm:"column:agent_file_retrieving_retry_times" json:"agent_file_retrieving_retry_times"`
	AgentFileRequestTimeout       int   `gorm:"column:agent_file_request_timeout" json:"agent_file_request_timeout"`
	AgentMaxWorkingProcesses      int   `gorm:"column:agent_max_working_processes" json:"agent_max_working_processes"`
	AgentMaxWorkingCapability     int   `gorm:"column:agent_max_working_capability" json:"agent_max_working_capability"`
	AgentMaxCores                 int   `gorm:"column:agent_max_cores" json:"agent_max_cores"`
	AgentMaxFileRetrievingCount   int   `gorm:"column:agent_max_file_retrieving_count" json:"agent_max_file_retrieving_count"`
	AgentNoCoordinator            bool  `gorm:"column:agent_no_coordinator;default:false" json:"agent_no_coordinator"`
	AgentFilePackageSize          int64 `gorm:"column:agent_file_package_size" json:"agent_file_package_size"`
	AgentChunkRetryInterval       int   `gorm:"column:agent_chunk_retry_interval" json:"agent_chunk_retry_interval"`
	AgentMaxTransmissionPeers     int   `gorm:"column:agent_max_transmission_peers" json:"agent_max_transmission_peers"`
	AgentMaxActiveTransmission    int   `gorm:"column:agent_max_active_transmission" json:"agent_max_active_transmission"`
	AgentEnableFileCompress       bool  `gorm:"column:agent_enable_file_compress" json:"agent_enable_file_compress"`
	AgentPrepareTimeout           int   `gorm:"column:agent_prepare_timeout" json:"agent_prepare_timeout"`
	AgentEnableFileLog            bool  `gorm:"column:agent_enable_file_log" json:"agent_enable_file_log"`
}

// TableName specific table name.
func (tps TableProjectSetting) TableName() string {
	return mysqlTableProjectSetting
}

// TableProjectInfo describe the db columns of project info.
// It will inherit the ProjectInfoBasic.
type TableProjectInfo struct {
	engine.TableProjectInfoBasic

	ServiceUnits float64 `gorm:"column:service_units" json:"service_units"`
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
