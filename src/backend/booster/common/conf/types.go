/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package conf

import (
	goflag "flag"
	"fmt"
	"io/ioutil"
	"os"
	"reflect"
	"strconv"
	"strings"
	"unsafe"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/util"

	"github.com/bitly/go-simplejson"
	"github.com/spf13/pflag"
)

// Config file, if set it will cover all the flag value it contains
type FileConfig struct {
	ConfigFile string `json:"file" short:"f" value:"" usage:"json file with configuration"`
}

// Log configuration
type LogConfig struct {
	LogDir     string `json:"log_dir" value:"./logs" usage:"If non-empty, write log files in this directory" mapstructure:"log_dir"`
	LogMaxSize uint64 `json:"log_max_size" value:"500" usage:"Max size (MB) per log file." mapstructure:"log_max_size"`
	LogMaxNum  int    `json:"log_max_num" value:"10" usage:"Max num of log file. The oldest will be removed if there is a extra file created." mapstructure:"log_max_num"`

	ToStdErr        bool   `json:"logtostderr" value:"false" usage:"log to standard error instead of files" mapstructure:"logtostderr"`
	StdErrLevel     int32  `json:"stderr_level" value:"0" usage:"logs to std error level, info=0, warning=1, error=2" mapstructure:"stderr_level"`
	AlsoToStdErr    bool   `json:"alsologtostderr" value:"false" usage:"log to standard error as well as files" mapstructure:"alsologtostderr"`
	Verbosity       int32  `json:"v" value:"0" usage:"log level for V logs" mapstructure:"v"`
	StdErrThreshold string `json:"stderrthreshold" value:"2" usage:"logs at or above this threshold go to stderr" mapstructure:"stderrthreshold"`
	VModule         string `json:"vmodule" value:"" usage:"comma-separated list of pattern=N settings for file-filtered logging" mapstructure:"vmodule"`
	TraceLocation   string `json:"log_backtrace_at" value:"" usage:"when logging hits line file:N, emit a stack trace" mapstructure:"log_backtrace_at"`
	AsyncFlush      bool   `json:"async_flush" value:"false" usage:"async flush file log"`
}

// Process configuration
type ProcessConfig struct {
	PidDir string `json:"pid_dir" value:"./pid" usage:"The dir for pid file" mapstructure:"pid_dir"`
}

// Service bind
type ServiceConfig struct {
	Address         string `json:"address" short:"a" value:"127.0.0.1" usage:"IP address to listen on for this service" mapstructure:"address"`
	Port            uint   `json:"port" short:"p" value:"8080" usage:"Port to listen on for this service" mapstructure:"port"`
	InsecureAddress string `json:"insecure_address" value:"127.0.0.1" usage:"insecure IP address to listen on for this service" mapstructure:"insecure_address"`
	InsecurePort    uint   `json:"insecure_port" value:"8443" usage:"insecure port to listen on for this service" mapstructure:"insecure_port"`
}

type CommonEngineConfig struct {
	KeeperStartingTimeout uint `json:"keep_starting_timeout_second" value:"120" usage:"timeout of starting task" mapstructure:"120"`
}

// Local info
type LocalConfig struct {
	LocalIP string `json:"local_ip" value:"" usage:"IP address of this host" mapstructure:"local_ip"`
}

// Metric info
type MetricConfig struct {
	MetricPort uint `json:"metric_port" value:"8081" usage:"Port to listen on for metric" mapstructure:"metric_port" `
}

// Register discover
type ZkConfig struct {
	BCSZk string `json:"bcs_zookeeper" value:"127.0.0.1:2181" usage:"Zookeeper server for registering and discovering" mapstructure:"bcs_zookeeper" `
}

// Server and client TLS config, can not be import with ClientCertOnlyConfig or ServerCertOnlyConfig
type CertConfig struct {
	CAFile         string `json:"ca_file" value:"" usage:"CA file. If server_cert_file/server_key_file/ca_file are all set, it will set up an HTTPS server required and verified client cert" mapstructure:"ca_file" value:""`
	ServerCertFile string `json:"server_cert_file" value:"" usage:"Server public key file(*.crt). If both server_cert_file and server_key_file are set, it will set up an HTTPS server" mapstructure:"server_cert_file"`
	ServerKeyFile  string `json:"server_key_file" value:"" usage:"Server private key file(*.key). If both server_cert_file and server_key_file are set, it will set up an HTTPS server" mapstructure:"server_key_file"`
	ClientCertFile string `json:"client_cert_file" value:"" usage:"Client public key file(*.crt)" mapstructure:"client_cert_file"`
	ClientKeyFile  string `json:"client_key_file" value:"" usage:"Client private key file(*.key)" mapstructure:"client_key_file" `
}

// Client TLS config, can not be import with CertConfig or ServerCertOnlyConfig
type ClientOnlyCertConfig struct {
	CAFile         string `json:"ca_file" value:"" usage:"CA file. If server_cert_file/server_key_file/ca_file are all set, it will set up an HTTPS server required and verified client cert"`
	ClientCertFile string `json:"client_cert_file" value:"" usage:"Client public key file(*.crt)"`
	ClientKeyFile  string `json:"client_key_file" value:"" usage:"Client private key file(*.key)"`
}

// Server TLS config, can not be import with ClientCertOnlyConfig or CertConfig
type ServerOnlyCertConfig struct {
	CAFile         string `json:"ca_file" value:"" usage:"CA file. If server_cert_file/server_key_file/ca_file are all set, it will set up an HTTPS server required and verified client cert"`
	ServerCertFile string `json:"server_cert_file" value:"" usage:"Server public key file(*.crt). If both server_cert_file and server_key_file are set, it will set up an HTTPS server"`
	ServerKeyFile  string `json:"server_key_file" value:"" usage:"Server private key file(*.key). If both server_cert_file and server_key_file are set, it will set up an HTTPS server"`
}

// License server config
type LicenseServerConfig struct {
	LSAddress        string `json:"ls_address" value:"" usage:"The license server address" mapstructure:"ls_address"`
	LSCAFile         string `json:"ls_ca_file" value:"" usage:"CA file for connecting to license server" mapstructure:"ls_ca_file"`
	LSClientCertFile string `json:"ls_client_cert_file" value:"" usage:"Client public key file(*.crt) for connecting to license server" mapstructure:"ls_client_cert_file"`
	LSClientKeyFile  string `json:"ls_client_key_file" value:"" usage:"Client private key file(*.key) for connecting to license server" mapstructure:"ls_client_key_file"`
}

// Parse parse config from file or command line
// Priority command line > file > default
func Parse(config interface{}) {
	// load config to flag
	loadRawConfig(pflag.CommandLine, config)

	// parse flags
	util.InitFlags()
	if err := goflag.CommandLine.Parse([]string{}); err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}

	// parse config file if exists
	if err := decJSON(pflag.CommandLine, config); err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
}

func decJSON(fs *pflag.FlagSet, config interface{}) error {
	f := reflect.ValueOf(config).Elem().FieldByName("ConfigFile").String()
	if f == "" {
		return nil
	}
	raw, err := ioutil.ReadFile(f)
	if err != nil {
		return err
	}
	jsn, err := simplejson.NewJson(raw)
	if err != nil {
		return err
	}

	removeLowPriorityKey(fs, jsn, reflect.TypeOf(config).Elem())

	safeRaw, err := jsn.MarshalJSON()
	if err != nil {
		return err
	}

	return codec.DecJSON(safeRaw, config)
}

func removeLowPriorityKey(fs *pflag.FlagSet, jsn *simplejson.Json, flagConfigType reflect.Type) {
	flagNames := make([]string, 0)
	n := flagConfigType.NumField()
	for i := 0; i < n; i++ {
		field := flagConfigType.Field(i)
		jsonName := field.Tag.Get("json")

		if jsonName == "" {
			switch field.Type.Kind() {
			case reflect.Struct:
				removeLowPriorityKey(fs, jsn, field.Type)
			case reflect.Ptr:
				removeLowPriorityKey(fs, jsn, field.Type.Elem())
			}
			continue
		}

		switch field.Type.Kind() {
		case reflect.Struct:
			removeLowPriorityKey(fs, jsn.Get(jsonName), field.Type)
			continue
		case reflect.Ptr:
			removeLowPriorityKey(fs, jsn.Get(jsonName), field.Type.Elem())
		default:
			flagNames = append(flagNames, jsonName)
		}
	}

	for _, fn := range flagNames {
		if fs.Changed(fn) {
			jsn.Del(fn)
		}
	}
}

// Make field to flag by adding "json" "value" "usage"
func loadRawConfig(fs *pflag.FlagSet, config interface{}) {
	wrap2flag(fs, reflect.TypeOf(config).Elem(), reflect.ValueOf(config).Elem())
}

func loadConfig(fs *pflag.FlagSet, configType reflect.Type, configValue reflect.Value) {
	wrap2flag(fs, configType, configValue)
}

func wrap2flag(fs *pflag.FlagSet, configType reflect.Type, configValue reflect.Value) {
	n := configType.NumField()

	for i := 0; i < n; i++ {
		field := configType.Field(i)
		fieldV := configValue.Field(i)
		if !fieldV.IsValid() || !fieldV.CanSet() {
			continue
		}

		flagName, nameOk := field.Tag.Lookup("json")
		if !nameOk && !field.Anonymous {
			continue
		}

		flagValue, valueOk := field.Tag.Lookup("value")
		flagUsage, usageOk := field.Tag.Lookup("usage")
		flagShortHand := field.Tag.Get("short")

		switch field.Type.Kind() {
		case reflect.Struct:
			loadConfig(fs, field.Type, fieldV)
			continue
		case reflect.Ptr:
			loadConfig(fs, field.Type.Elem(), fieldV.Elem())
			continue
		}
		// flag must have "json, value, usage" flags
		// json and flag can not be empty
		if !nameOk || !valueOk || !usageOk || flagName == "" || flagUsage == "" {
			continue
		}

		unsafePtr := unsafe.Pointer(fieldV.UnsafeAddr())
		switch field.Type.Kind() {
		case reflect.String:
			fs.StringVarP((*string)(unsafePtr), flagName, flagShortHand, flagValue, flagUsage)
		case reflect.Bool:
			v := flagValue == "true"
			fs.BoolVarP((*bool)(unsafePtr), flagName, flagShortHand, v, flagUsage)
		case reflect.Uint:
			v, _ := strconv.ParseUint(flagValue, 10, 0)
			fs.UintVarP((*uint)(unsafePtr), flagName, flagShortHand, uint(v), flagUsage)
		case reflect.Uint32:
			v, _ := strconv.ParseUint(flagValue, 10, 32)
			fs.Uint32VarP((*uint32)(unsafePtr), flagName, flagShortHand, uint32(v), flagUsage)
		case reflect.Uint64:
			v, _ := strconv.ParseUint(flagValue, 10, 64)
			fs.Uint64VarP((*uint64)(unsafePtr), flagName, flagShortHand, uint64(v), flagUsage)
		case reflect.Int:
			v, _ := strconv.ParseInt(flagValue, 10, 0)
			fs.IntVarP((*int)(unsafePtr), flagName, flagShortHand, int(v), flagUsage)
		case reflect.Int32:
			v, _ := strconv.ParseInt(flagValue, 10, 32)
			fs.Int32VarP((*int32)(unsafePtr), flagName, flagShortHand, int32(v), flagUsage)
		case reflect.Int64:
			v, _ := strconv.ParseInt(flagValue, 10, 64)
			fs.Int64VarP((*int64)(unsafePtr), flagName, flagShortHand, int64(v), flagUsage)
		case reflect.Float32:
			v, _ := strconv.ParseFloat(flagValue, 32)
			fs.Float32VarP((*float32)(unsafePtr), flagName, flagShortHand, float32(v), flagUsage)
		case reflect.Float64:
			v, _ := strconv.ParseFloat(flagValue, 64)
			fs.Float64VarP((*float64)(unsafePtr), flagName, flagShortHand, float64(v), flagUsage)
		case reflect.Slice:
			arr := make([]string, 0)
			if flagValue != "" {
				arr = strings.Split(flagValue, ",")
			}
			switch field.Type.Elem().Kind() {
			case reflect.String:
				fs.StringSliceVarP((*[]string)(unsafePtr), flagName, flagShortHand, arr, flagUsage)
			case reflect.Int:
				intArr := make([]int, 0, len(arr))
				for _, si := range arr {
					ii, _ := strconv.ParseInt(si, 10, 0)
					intArr = append(intArr, int(ii))
				}
				fs.IntSliceVarP((*[]int)(unsafePtr), flagName, flagShortHand, intArr, flagUsage)
			}
		default:
			continue
		}
	}
	return
}
