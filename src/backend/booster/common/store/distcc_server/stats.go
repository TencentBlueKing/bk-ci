/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package ds

import (
	"reflect"
	"strconv"
	"strings"
	"unsafe"
)

// StatsInfo describe the stats get from distcc daemon
type StatsInfo struct {
	TCPAccept           int64   `json:"dcc_tcp_accept"`
	RejBadReq           int64   `json:"dcc_rej_bad_req"`
	RejOverload         int64   `json:"dcc_rej_overload"`
	CompileOK           int64   `json:"dcc_compile_ok"`
	CompileErr          int64   `json:"dcc_compile_error"`
	CompileTimeout      int64   `json:"dcc_compile_timeout"`
	CliDisconnect       int64   `json:"dcc_cli_disconnect"`
	Other               int64   `json:"dcc_other"`
	LongestJob          string  `json:"dcc_longest_job"`
	LongestJobCompiler  string  `json:"dcc_longest_job_compiler"`
	LongestJobTimeMsecs string  `json:"dcc_longest_job_time_msecs"`
	MaxKids             int64   `json:"dcc_max_kids"`
	AvgKids1            float64 `json:"dcc_avg_kids1"`
	AvgKids2            float64 `json:"dcc_avg_kids2"`
	AvgKids3            float64 `json:"dcc_avg_kids3"`
	CurrentLoad         int64   `json:"dcc_current_load"`
	Load1               float64 `json:"dcc_load1"`
	Load2               float64 `json:"dcc_load2"`
	Load3               float64 `json:"dcc_load3"`
	NumCompiles1        int64   `json:"dcc_num_compiles1"`
	NumCompiles2        int64   `json:"dcc_num_compiles2"`
	NumCompiles3        int64   `json:"dcc_num_compiles3"`
	NumProcstateD       int64   `json:"dcc_num_procstate_D"`
	MaxRSS              int64   `json:"dcc_max_RSS"`
	MaxRSSName          string  `json:"dcc_max_RSS_name"`
	IORate              int64   `json:"dcc_io_rate"`
	FreeSpace           string  `json:"dcc_free_space"`
}

// ParseAll parse the message from distcc daemon into StatsInfo
func (si *StatsInfo) ParseAll(data []byte) {
	lines := strings.Split(string(data), "\n")
	for _, line := range lines {
		si.Parse(line)
	}
}

// Parse parse the single line stats message
func (si *StatsInfo) Parse(line string) {
	line = strings.Replace(line, "\n", "", -1)
	line = strings.Replace(line, "\r", "", -1)

	if line == "" || line == "<distccstats>" || line == "</distccstats>" || line == "argv /distccd" {
		return
	}

	items := strings.Split(line, " ")
	if len(items) < 2 {
		return
	}
	key := items[0]
	value := strings.Trim(line[len(key):], " ")

	infoType := reflect.TypeOf(si).Elem()
	infoValue := reflect.ValueOf(si).Elem()
	n := infoType.NumField()

	for i := 0; i < n; i++ {
		field := infoType.Field(i)
		fieldV := infoValue.Field(i)
		if v, ok := field.Tag.Lookup("json"); ok && (v == key) {
			unsafePtr := unsafe.Pointer(fieldV.UnsafeAddr())
			switch field.Type.Kind() {
			case reflect.String:
				*(*string)(unsafePtr) = value
			case reflect.Int64:
				*(*int64)(unsafePtr), _ = strconv.ParseInt(value, 10, 64)
			case reflect.Float64:
				*(*float64)(unsafePtr), _ = strconv.ParseFloat(value, 64)
			}
			break
		}
	}
}

// GetCurrentLoad return the current running job number according to stats
func (si *StatsInfo) GetCurrentLoad() *StatsLoad {
	return &StatsLoad{
		StatsInfo: si,
		RunningJobs: si.TCPAccept - si.RejBadReq - si.RejOverload - si.CompileOK -
			si.CompileErr - si.CompileTimeout - si.CliDisconnect - si.Other,
	}
}

// StatsLoad describe the stats info and the job running status
type StatsLoad struct {
	*StatsInfo
	RunningJobs int64
}
