package config

import (
	"strings"

	"build-booster/common/conf"
	"build-booster/common/encrypt"
	"build-booster/common/net"
	"build-booster/common/static"
)

// ServerConfig
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig
	conf.MetricConfig

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
	DistCCQueueList    []string           `json:"distcc_queue_list" value:"[]" usage:"queue name list for engine distcc"`
	EngineDistCCConfig EngineDistCCConfig `json:"engine_distcc"`

	DisttaskQueueList    []string             `json:"disttask_queue_list" value:"[]" usage:"queue name list for engine disttask"`
	EngineDisttaskConfig EngineDisttaskConfig `json:"engine_disttask"`

	DistccMacQueueList    []string              `json:"distcc_mac_queue_list" value:"[]" usage:"queue name list for engine distcc_mac"`
	EngineDistCCMacConfig EngineDistCCMacConfig `json:"engine_distcc_mac"`

	// engine fastbuild settings
	FastBuildQueueList    []string              `json:"fastbuild_queue_list" value:"[]" usage:"queue name list for engine fastbuild"`
	EngineFastBuildConfig EngineFastBuildConfig `json:"engine_fastbuild"`

	// resource manager
	DirectResourceConfig DirectResourceConfig `json:"direct_resource"`

	ContainerResourceConfig ContainerResourceConfig `json:"container_resource"`

	K8sContainerResourceConfig ContainerResourceConfig `json:"k8s_container_resource"`

	// cert of the server
	ServerCert *CertConfig
}

// DirectResourceConfig defines configs for resource which agent connect to us directly.
type DirectResourceConfig struct {
	Enable     bool   `json:"direct_enable" value:"false" usage:"enable direct resource manager"`
	ListenPort uint   `json:"direct_resource_port" value:"" usage:"port to listen for direct resource agent report"`
	ListenIP   string `json:"direct_resource_ip" value:"" usage:"ip to listen for direct resource agent report"`
	ServerCert *CertConfig // cert of the server

	Agent4OneTask bool `json:"agent_4_one_task" value:"true" usage:"if set true, one agent will only be used by one task,no matter it has free resource"`

	MySQLStorage     string `json:"direct_resource_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase    string `json:"direct_resource_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"direct_resource_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"direct_resource_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MysqlTableOption string `json:"direct_resource_mysql_table_option" value:"" usage:"mysql table option"`
}

// ContainerResourceConfig defines configs for resource from bcs.
type ContainerResourceConfig struct {
	Enable            bool    `json:"crm_enable"`
	Operator          string  `json:"crm_operator"`
	BcsAPIToken       string  `json:"crm_bcs_api_token"`
	BcsAPIAddress     string  `json:"crm_bcs_api_address"`
	BcsCPUPerInstance float64 `json:"crm_bcs_cpu_per_instance"`
	BcsMemPerInstance float64 `json:"crm_bcs_mem_per_instance"`
	BcsClusterID      string  `json:"crm_bcs_cluster_id"`
	BcsAppTemplate    string  `json:"crm_bcs_template_file"`

	MySQLStorage     string `json:"crm_resource_mysql"`
	MySQLDatabase    string `json:"crm_resource_mysql_db"`
	MySQLTable       string `json:"crm_resource_mysql_table"`
	MySQLUser        string `json:"crm_resource_mysql_user"`
	MySQLPwd         string `json:"crm_resource_mysql_pwd"`
	MysqlTableOption string `json:"crm_resource_mysql_table_option"`

	BcsAPIPool *net.ConnectPool
}

const (
	CRMOperatorMesos = "mesos"
	CRMOperatorK8S   = "k8s"
)

// EngineDistCCConfig define the distcc engine config.
type EngineDistCCConfig struct {
	Enable bool `json:"engine_distcc_enable" value:"false" usage:"enable engine distcc"`

	MySQLStorage     string `json:"engine_distcc_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase    string `json:"engine_distcc_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_distcc_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_distcc_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_distcc_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_distcc_mysql_table_option" value:"" usage:"mysql table option"`

	LeastJobServer      int                        `json:"least_job_server" value:"144" usage:"least job server for remote compiles"`
	JobServerTimesToCPU float64                    `json:"job_server_times_to_cpu" value:"1.5" usage:"job server times to cpu"`
	BrokerConfig        []EngineDistCCBrokerConfig `json:"broker_config"`
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

	MySQLStorage     string `json:"engine_disttask_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase    string `json:"engine_disttask_mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser        string `json:"engine_disttask_mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd         string `json:"engine_disttask_mysql_pwd" value:"" usage:"mysql password, encrypted"`
	MySQLDebug       bool   `json:"engine_disttask_mysql_debug" value:"false" usage:"if true, will output raw sql"`
	MysqlTableOption string `json:"engine_disttask_mysql_table_option" value:"" usage:"mysql table option"`

	LeastJobServer      int                          `json:"disttask_least_job_server" value:"144" usage:"least job server for remote compiles"`
	JobServerTimesToCPU float64                      `json:"disttask_job_server_times_to_cpu" value:"1.5" usage:"job server times to cpu"`
	BrokerConfig        []EngineDisttaskBrokerConfig `json:"disttask_broker_config"`
}

// EngineDisttaskBrokerConfig define the broker config used by engine disttask.
type EngineDisttaskBrokerConfig struct {
	WorkerVersion  string `json:"worker_version"`
	Scene          string `json:"scene"`
	City           string `json:"city"`
	Instance       int    `json:"instance"`
	ConstNum       int    `json:"const_num"`
	JobPerInstance int    `json:"job_per_instance"`
	Allow          string `json:"allow"`
}

// EngineDisttaskQueueInstanceConfig define the specific config for some queue.
type EngineDisttaskQueueConfig struct {
	QueueName      string  `json:"queue_name"`
	CPUPerInstance float64 `json:"cpu_per_instance"`
	MemPerInstance float64 `json:"mem_per_instance"`
}

// EngineDistCCMacConfig define the distcc_mac engine config.
type EngineDistCCMacConfig struct {
	Enable bool `json:"engine_distcc_mac_enable" value:"false" usage:"enable engine distcc_mac"`

	MySQLStorage     string `json:"engine_distcc_mac_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
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

	MySQLStorage     string `json:"engine_fastbuild_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
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

	MySQLStorage     string `json:"engine_apisjob_mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
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
		dsc.ContainerResourceConfig.BcsAPIPool = net.NewConnectPool(strings.Split(dsc.ContainerResourceConfig.BcsAPIAddress, ","))
		dsc.ContainerResourceConfig.BcsAPIPool.Start()
	}

	if dsc.K8sContainerResourceConfig.Enable {
		tokenK8S, _ := encrypt.DesDecryptFromBase([]byte(dsc.K8sContainerResourceConfig.BcsAPIToken))
		dsc.K8sContainerResourceConfig.BcsAPIToken = string(tokenK8S)
		dsc.K8sContainerResourceConfig.BcsAPIPool = net.NewConnectPool(strings.Split(dsc.K8sContainerResourceConfig.BcsAPIAddress, ","))
		dsc.K8sContainerResourceConfig.BcsAPIPool.Start()
	}
}
