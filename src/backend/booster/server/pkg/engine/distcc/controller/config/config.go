/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */
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
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/conf"
	"github.com/Tencent/bk-ci/src/booster/common/static"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/pkg/types"
)

// DistCCControllerConfig describe the controller server config
type DistCCControllerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig

	EtcdEndpoints string `json:"etcd_endpoints" value:"" usage:"etcd endpoints for register and discover"`
	EtcdRootPath  string `json:"etcd_root_path" value:"" usage:"etcd root path"`
	EtcdCaFile    string `json:"etcd_ca_file" value:"" usage:"etcd ca file"`
	EtcdCertFile  string `json:"etcd_cert_file" value:"" usage:"etcd cert file"`
	EtcdKeyFile   string `json:"etcd_key_file" value:"" usage:"etcd key file"`
	EtcdKeyPwd    string `json:"etcd_key_password" value:"" usage:"etcd key password"`

	MySQLStorage  string `json:"mysql" value:"" usage:"mysql address for storage, e.g. 127.0.0.1:3306"`
	MySQLDatabase string `json:"mysql_db" value:"" usage:"mysql database for connecting."`
	MySQLUser     string `json:"mysql_user" value:"root" usage:"mysql username"`
	MySQLPwd      string `json:"mysql_pwd" value:"" usage:"mysql password, encrypted"`

	StrategyLevel int     `json:"strategy_level" value:"1" usage:"strategy level, 0: Never, 1: 1week10times, 2: 2weeks20times, 3: 1month30times"`
	CPUUnit       int64   `json:"cpu_unit" value:"8" usage:"the least cpu and the range between two level of resources"`
	CPUMaxLevel   int     `json:"cpu_max_level" value:"18" usage:"the max cpu level, the max cpu should be cpu_unit*cpu_max_level"`
	CPURedundancy float64 `json:"cpu_redundancy" value:"1.5" usage:"the redundancy times of max cpu"`

	InspectTaskGap            int `json:"inspect_task_gap" value:"5" usage:"list the running tasks every per inspect_task_gap seconds"`
	InspectStatsGap           int `json:"inspect_stats_gap" value:"500" usage:"inspect the distcc stats every per inspect_stats_gap milliseconds"`
	CheckProjectGap           int `json:"check_project_gap" value:"300" usage:"check the recent projects every per check_project_gap seconds"`
	LastSuggestionAcceptedGap int `json:"last_suggestion_accepted_gap" value:"7" usage:"the suggestion should be accepted for last_suggestion_accepted_gap days before the next"`

	OperationWhiteList  []string `json:"operation_whitelist" value:"" usage:"whitelist of projectID for operation, blank for all"`
	SuggestionWhiteList []string `json:"suggestion_whitelist" value:"" usage:"whitelist of projectID for suggestion, blank for all"`
	GccVersionBlackList []string `json:"gcc_version_blacklist" value:"" usage:"blacklist of gccVersion for watching, blank for none"`

	ServerCert *CertConfig // cert of the server
}

// CertConfig  configuration of Cert
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// NewConfig get a new DistCCControllerConfig
func NewConfig() *DistCCControllerConfig {
	return &DistCCControllerConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

// Parse DistCCControllerConfig from file or command line
func (dsc *DistCCControllerConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}

	// init time
	types.InspectRunningTaskTimeGap = time.Duration(dsc.InspectTaskGap) * time.Second
	types.InspectDistCCStatTimeGap = time.Duration(dsc.InspectStatsGap) * time.Millisecond
	types.CheckProjectTimeGap = time.Duration(dsc.CheckProjectGap) * time.Second
	types.LastSuggestionAcceptedTimeGap = time.Duration(dsc.LastSuggestionAcceptedGap) * 24 * time.Hour
}
