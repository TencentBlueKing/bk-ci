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
	"runtime"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/conf"
	"github.com/Tencent/bk-ci/src/booster/common/static"
)

// ServerConfig : server config
type ServerConfig struct {
	conf.FileConfig
	conf.ServiceConfig
	conf.LogConfig
	conf.ProcessConfig
	conf.ServerOnlyCertConfig
	conf.LocalConfig

	ServerCert *CertConfig // cert of the server

	LocalSlots     int `json:"local_slots" value:"0" usage:"local slots define the max slots controller can get from localhost, default is NumCPU-2"`
	LocalPreSlots  int `json:"local_pre_slots" value:"0" usage:"local pre slots define the max slots controller can use for pre work, default is up to local_slots"`
	LocalExeSlots  int `json:"local_exe_slots" value:"0" usage:"local exe slots define the max slots controller can use for exe work, default is up to local_slots"`
	LocalPostSlots int `json:"local_post_slots" value:"0" usage:"local post slots define the max slots controller can use for post work, default is up to local_slots"`
	RemainTime     int `json:"remain_time" value:"120" usage:"controller remain time after there is no active work (seconds)"`

	NoWait             bool `json:"no_wait" value:"false" usage:"if true, controller will quit immediately when no more running task"`
	UseLocalCPUPercent int  `json:"use_local_cpu_percent" value:"0" usage:"how many local idle cpu will be used to execute tasks(0~100)"`
	DisableFileLock    bool `json:"disable_file_lock" value:"false" usage:"if true, controller will launch without file lock"`

	AutoResourceMgr    bool `json:"auto_resource_mgr" value:"false" usage:"if true, controller will auto free and apply resource while work running"`
	ResIdleSecsForFree int  `json:"res_idle_secs_for_free" value:"120" usage:"controller free resource while detect resource has been idle over this"`

	SendCork bool `json:"send_cork" value:"false" usage:"if true, controller will send files like tcp cork"`

	NetErrorLimit    int `json:"net_error_limit" value:"false" usage:"if true, controller will send files like tcp cork"`
	RemoteRetryTimes int `json:"remote_retry_times" value:"false" usage:"if true, controller will send files like tcp cork"`
}

// CertConfig  configuration of Cert
type CertConfig struct {
	CAFile   string
	CertFile string
	KeyFile  string
	CertPwd  string
	IsSSL    bool
}

// NewConfig : return config of server
func NewConfig() *ServerConfig {
	return &ServerConfig{
		ServerCert: &CertConfig{
			CertPwd: static.ServerCertPwd,
			IsSSL:   false,
		},
	}
}

// Parse : parse server config
func (dsc *ServerConfig) Parse() {
	conf.Parse(dsc)

	dsc.ServerCert.CertFile = dsc.ServerCertFile
	dsc.ServerCert.KeyFile = dsc.ServerKeyFile
	dsc.ServerCert.CAFile = dsc.CAFile

	if dsc.ServerCert.CertFile != "" && dsc.ServerCert.KeyFile != "" {
		dsc.ServerCert.IsSSL = true
	}

	// default local slots is NumCPU-2.
	if dsc.LocalSlots == 0 {
		dsc.LocalSlots = runtime.NumCPU() - 2
	}

	// local slots must be positive.
	if dsc.LocalSlots <= 0 {
		dsc.LocalSlots = 1
	}

	if dsc.LocalPreSlots <= 0 || dsc.LocalPreSlots > dsc.LocalSlots {
		dsc.LocalPreSlots = dsc.LocalSlots
	}
	if dsc.LocalExeSlots <= 0 || dsc.LocalExeSlots > dsc.LocalSlots {
		dsc.LocalExeSlots = dsc.LocalSlots
	}
	if dsc.LocalPostSlots <= 0 || dsc.LocalPostSlots > dsc.LocalSlots {
		dsc.LocalPostSlots = dsc.LocalSlots
	}
}

// Dump encode ServerConfig into json bytes
func (dsc *ServerConfig) Dump() []byte {
	var data []byte
	_ = codec.EncJSON(dsc, &data)
	return data
}
