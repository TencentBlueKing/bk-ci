/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package config

import (
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/conf"
	"github.com/Tencent/bk-ci/src/booster/common/encrypt"
	"github.com/Tencent/bk-ci/src/booster/common/net"
	"github.com/Tencent/bk-ci/src/booster/common/static"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
)

// ServerConfig define
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig
	conf.MetricConfig
	conf.CommonEngineConfig

	EtcdEndpoints string `json:"etcd_endpoints" value:"" usage:"etcd endpoints for register and discover"`
	EtcdRootPath  string `json:"etcd_root_path" value:"" usage:"etcd root path"`
	EtcdCaFile    string `json:"etcd_ca_file" value:"" usage:"etcd ca file"`
	EtcdCertFile  string `json:"etcd_cert_file" value:"" usage:"etcd cert file"`
	EtcdKeyFile   string `json:"etcd_key_file" value:"" usage:"etcd key file"`
	EtcdKeyPwd    string `json:"etcd_key_password" value:"" usage:"etcd key password"`

	DebugMode bool `json:"debug" value:"false" usage:"*ATTENTION* debug mode will disable some handlers such as task heartbeat checking"`

	// engine apisjobs settings
	ApisJobQueueList    []string            `json:"apisjob_queue_list" value:"[]" usage:"queue name list for engine apisjob"`
	EngineApisJobConfig EngineApisJobConfig `json:"engine_apisjob"`

	// engine distcc settings
	DistCCQueueList      []string                         `json:"distcc_queue_list" value:"[]" usage:"queue name list for engine distcc"`
	DistccQueueShareType map[string]engine.QueueShareType `json:"distcc_queue_share_type" usage:"queue name map for share type, default is all allowed"`
	EngineDistCCConfig   EngineDistCCConfig               `json:"engine_distcc"`

	DisttaskQueueList      []string                         `json:"disttask_queue_list" value:"[]" usage:"queue name list for engine disttask"`
	DisttaskQueueShareType map[string]engine.QueueShareType `json:"disttask_queue_share_type" usage:"queue name map for share type, default is all allowed"`
	EngineDisttaskConfig   EngineDisttaskConfig             `json:"engine_disttask"`

	DistccMacQueueList    []string              `json:"distcc_mac_queue_list" value:"[]" usage:"queue name list for engine distcc_mac"`
	EngineDistCCMacConfig EngineDistCCMacConfig `json:"engine_distcc_mac"`

	// engine fastbuild settings
	FastBuildQueueList    []string              `json:"fastbuild_queue_list" value:"[]" usage:"queue name list for engine fastbuild"`
	EngineFastBuildConfig EngineFastBuildConfig `json:"engine_fastbuild"`

	// resource manager
	DirectResourceConfig DirectResourceConfig `json:"direct_resource"`

	ContainerResourceConfig ContainerResourceConfig `json:"container_resource"`

	K8sContainerResourceConfig ContainerResourceConfig `json:"k8s_container_resource"`

	K8sResourceConfigList K8sResourceConfig `json:"k8s_resource_list"`

	DCMacContainerResourceConfig ContainerResourceConfig `json:"dc_mac_container_resource"`

	// cert of the server
	ServerCert *CertConfig
}

// DirectResourceConfig defines configs for resource which agent connect to us directly.
type DirectResourceConfig struct {
	Enable     bool        `json:"direct_enable" value:"false" usage:"enable direct resource manager"`
	ListenPort uint        `json:"direct_resource_port" value:"" usage:"port to listen for direct resource agent report"`
	ListenIP   string      `json:"direct_resource_ip" value:"" usage:"ip to listen for direct resource agent report"`
	ServerCert *CertConfig // cert of the server

	Agent4OneTask bool `json:"agent_4_one_task" value:"true" usage:"if set true, one agent will only be used by one task,no matter it has free resource"`

	MySQLStorage     string `json:"direct_resource_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"direct_resource_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"direct_resource_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"direct_resource_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MysqlTableOption string `json:"direct_resource_mysql_table_option" value:"" usage:"mysql table option"`
}

//InstanceType define type of an instance
type InstanceType struct {
	Platform            string  `json:"platform"`
	Group               string  `json:"group"`
	CPUPerInstance      float64 `json:"cpu_per_instance"`
	MemPerInstance      float64 `json:"mem_per_instance"`
	CPULimitPerInstance float64 `json:"cpu_limit_per_instance,omitempty"`
	MemLimitPerInstance float64 `json:"mem_limit_per_instance,omitempty"`
}

// ContainerResourceConfig defines configs for resource from bcs.
type ContainerResourceConfig struct {
	Enable                     bool           `json:"crm_enable"`
	Operator                   string         `json:"crm_operator"`
	BcsAPIToken                string         `json:"crm_bcs_api_token"`
	BcsAPIAddress              string         `json:"crm_bcs_api_address"`
	BcsNamespace               string         `json:"crm_bcs_namespace"`
	EnableBCSApiGw             bool           `json:"crm_bcs_apigw_enable" value:"false"`
	BcsCPUPerInstance          float64        `json:"crm_bcs_cpu_per_instance"`
	BcsMemPerInstance          float64        `json:"crm_bcs_mem_per_instance"`
	BcsStoragePerInstance      float64        `json:"crm_bcs_storage_per_instance,omitempty"`
	BcsCPULimitPerInstance     float64        `json:"crm_bcs_cpu_limit_per_instance,omitempty"`
	BcsMemLimitPerInstance     float64        `json:"crm_bcs_mem_limit_per_instance,omitempty"`
	BcsStorageLimitPerInstance float64        `json:"crm_bcs_storage_limit_per_instance,omitempty"`
	InstanceType               []InstanceType `json:"instance_type"`
	BcsClusterID               string         `json:"crm_bcs_cluster_id"`
	BcsClusterType             string         `json:"crm_bcs_cluster_type"`
	BcsAppTemplate             string         `json:"crm_bcs_template_file"`
	BcsGroupLabelKey           string         `json:"crm_bcs_group_label_key"`
	BcsPlatformLabelKey        string         `json:"crm_bcs_platform_label_key"`
	BcsDisableWinHostNW        bool           `json:"crm_bcs_disable_win_host_network"`

	MySQLStorage     string `json:"crm_resource_mysql"`
	MySQLDatabase    string `json:"crm_resource_mysql_db"`
	MySQLTable       string `json:"crm_resource_mysql_table"`
	MySQLUser        string `json:"crm_resource_mysql_user"`
	MySQLPwd         string `json:"crm_resource_mysql_pwd"`
	MysqlTableOption string `json:"crm_resource_mysql_table_option"`
	MysqlSkipEnsure  bool   `json:"crm_resource_mysql_skip_ensure"`

	BcsAPIPool *net.ConnectPool
}

// K8sResourceConfig define new k8s with cluster list
type K8sResourceConfig struct {
	Enable         bool                                `json:"crm_enable"`
	Operator       string                              `json:"crm_operator"`
	K8sClusterList map[string]*ContainerResourceConfig `json:"k8s_cluster_list"`

	MySQLStorage     string `json:"crm_resource_mysql"`
	MySQLDatabase    string `json:"crm_resource_mysql_db"`
	MySQLTable       string `json:"crm_resource_mysql_table"`
	MySQLUser        string `json:"crm_resource_mysql_user"`
	MySQLPwd         string `json:"crm_resource_mysql_pwd"`
	MysqlTableOption string `json:"crm_resource_mysql_table_option"`
	MysqlSkipEnsure  bool   `json:"crm_resource_mysql_skip_ensure"`
}

const (
	//CRMOperatorMesos define
	CRMOperatorMesos = "mesos"
	//CRMOperatorK8S define
	CRMOperatorK8S = "k8s"
	//CRMOperatorDCMac define
	CRMOperatorDCMac = "dc_mac"
)

// EngineDistCCConfig define the distcc engine config.
type EngineDistCCConfig struct {
	Enable bool `json:"engine_distcc_enable" value:"false" usage:"enable engine distcc"`

	MySQLStorage     string `json:"engine_distcc_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"engine_distcc_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_distcc_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_distcc_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_distcc_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_distcc_mysql_table_option" value:"" usage:"mysql table option"`

	QueueResourceAllocater map[string]ResourceAllocater `json:"queue_resource_allocater"`
	LeastJobServer         int                          `json:"least_job_server" value:"144" usage:"least job server for remote compiles"`
	JobServerTimesToCPU    float64                      `json:"job_server_times_to_cpu" value:"1.5" usage:"job server times to cpu"`
	BrokerConfig           []EngineDistCCBrokerConfig   `json:"broker_config"`
}

// EngineDistCCBrokerConfig define the broker config used by engine distcc.
type EngineDistCCBrokerConfig struct {
	GccVersion     string `json:"gcc_version"`
	City           string `json:"city"`
	Instance       int    `json:"instance"`
	ConstNum       int    `json:"const_num"`
	JobPerInstance int    `json:"job_per_instance"`
	Allow          string `json:"allow"`
}

// EngineDisttaskConfig define the disttask engine config.
type EngineDisttaskConfig struct {
	Enable bool `json:"engine_disttask_enable" value:"false" usage:"enable engine disttask"`

	MySQLStorage     string `json:"engine_disttask_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"engine_disttask_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_disttask_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_disttask_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_disttask_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_disttask_mysql_table_option" value:"" usage:"mysql table option"`

	QueueResourceAllocater map[string]ResourceAllocater `json:"queue_resource_allocater"`
	LeastJobServer         int                          `json:"disttask_least_job_server" value:"144" usage:"least job server for remote compiles"`
	JobServerTimesToCPU    float64                      `json:"disttask_job_server_times_to_cpu" value:"1.5" usage:"job server times to cpu"`
	BrokerConfig           []EngineDisttaskBrokerConfig `json:"disttask_broker_config"`
}

// ResourceAllocater define
type ResourceAllocater struct {
	AllocateByTimeMap map[string]float64 `json:"allocate_by_time_map"`
	TimeSlot          []TimeSlot         `json:"time_slot"`
}

// EngineDisttaskBrokerConfig define the broker config used by engine disttask.
type EngineDisttaskBrokerConfig struct {
	WorkerVersion   string                        `json:"worker_version"`
	Scene           string                        `json:"scene"`
	City            string                        `json:"city"`
	Instance        int                           `json:"instance"`
	ConstNum        int                           `json:"const_num"`
	IdleKeepSeconds int                           `json:"idle_keep_seconds" value:"-1" usage:"seconds to keep with idle status before release"`
	ReleaseLoop     bool                          `json:"release_loop" value:"false" usage:"whether realease resource with loop until succeed"`
	JobPerInstance  int                           `json:"job_per_instance"`
	Allow           string                        `json:"allow"`
	Volumes         []EngineDisttaskBrokerVolumes `json:"volumes"`
}

// EngineDisttaskBrokerVolumes describe the volumes for broker
type EngineDisttaskBrokerVolumes struct {
	HostDir      string `json:"host_dir"`
	ContainerDir string `json:"container_dir"`
}

// EngineDisttaskQueueConfig define the specific config for some queue.
type EngineDisttaskQueueConfig struct {
	QueueName      string  `json:"queue_name"`
	CPUPerInstance float64 `json:"cpu_per_instance"`
	MemPerInstance float64 `json:"mem_per_instance"`
}

// EngineDistCCMacConfig define the distcc_mac engine config.
type EngineDistCCMacConfig struct {
	Enable bool `json:"engine_distcc_mac_enable" value:"false" usage:"enable engine distcc_mac"`

	MySQLStorage     string `json:"engine_distcc_mac_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"engine_distcc_mac_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_distcc_mac_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_distcc_mac_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_distcc_mac_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_distcc_mac_mysql_table_option" value:"" usage:"mysql table option"`

	LeastJobServer      int     `json:"engine_distcc_mac_least_job_server" value:"144" usage:"least job server for remote compiles"`
	JobServerTimesToCPU float64 `json:"engine_distcc_mac_job_server_times_to_cpu" value:"1.5" usage:"job server times to cpu"`
}

// EngineFastBuildConfig define the fastbuild engine config.
type EngineFastBuildConfig struct {
	Enable bool `json:"engine_fastbuild_enable" value:"false" usage:"enable engine fastbuild"`

	MySQLStorage     string `json:"engine_fastbuild_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"engine_fastbuild_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_fastbuild_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_fastbuild_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_fastbuild_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_fastbuild_mysql_table_option" value:"" usage:"mysql table option"`

	TaskMaxRunningSeconds             int32  `json:"task_max_running_seconds" value:"" usage:"task max running seconds for fastbuild"`
	TaskBKMainNoSubTaskTimeoutSeconds int32  `json:"task_bkmain_nosubtask_timeout_seconds" value:"" usage:"task timeout seconds for bk-main no subtask"`
	SpecialFBCmd                      string `json:"special_fb_cmd" value:"" usage:"special fb cmd"`
}

// EngineApisJobConfig define the apis engine config.
type EngineApisJobConfig struct {
	Enable bool `json:"engine_apisjob_enable" value:"false" usage:"enable engine apisjob"`

	QueueResourceAllocater map[string]ResourceAllocater `json:"queue_resource_allocater"`

	MySQLStorage     string `json:"engine_apisjob_mysql" value:"" usage:"mysql address for storage"`
	MySQLDatabase    string `json:"engine_apisjob_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_apisjob_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_apisjob_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_apisjob_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_apisjob_mysql_table_option" value:"" usage:"mysql table option"`
}

// CertConfig configuration of Cert.
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// TimeSlot define resource ratio Value from StartTime to EndTime
type TimeSlot struct {
	StartTime string
	EndTime   string
	Value     float64
}

// NewConfig get a default server configuration.
func NewConfig() *ServerConfig {
	return &ServerConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

// Parse parse flags from commands and the config file and make some initialization.
func (dsc *ServerConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}

	if dsc.DirectResourceConfig.ServerCert == nil {
		dsc.DirectResourceConfig.ServerCert = dsc.ServerCert
	}

	if dsc.ContainerResourceConfig.Enable {
		token, _ := encrypt.DesDecryptFromBase([]byte(dsc.ContainerResourceConfig.BcsAPIToken))
		dsc.ContainerResourceConfig.BcsAPIToken = string(token)
		dsc.ContainerResourceConfig.BcsAPIPool = net.NewConnectPool(
			strings.Split(dsc.ContainerResourceConfig.BcsAPIAddress, ","))
		dsc.ContainerResourceConfig.BcsAPIPool.Start()
	}

	if dsc.K8sContainerResourceConfig.Enable {
		tokenK8S, _ := encrypt.DesDecryptFromBase([]byte(dsc.K8sContainerResourceConfig.BcsAPIToken))
		dsc.K8sContainerResourceConfig.BcsAPIToken = string(tokenK8S)
		dsc.K8sContainerResourceConfig.BcsAPIPool = net.NewConnectPool(
			strings.Split(dsc.K8sContainerResourceConfig.BcsAPIAddress, ","))
		dsc.K8sContainerResourceConfig.BcsAPIPool.Start()
	}

	if dsc.K8sResourceConfigList.Enable {
		for _, cluster := range dsc.K8sResourceConfigList.K8sClusterList {
			tokenK8S, _ := encrypt.DesDecryptFromBase([]byte(cluster.BcsAPIToken))
			cluster.BcsAPIToken = string(tokenK8S)
			cluster.BcsAPIPool = net.NewConnectPool(strings.Split(cluster.BcsAPIAddress, ","))
			cluster.BcsAPIPool.Start()
		}
	}
}
