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
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
)

const (
	BaseDir = "/etc/bk_dist"
	RunDir  = ".tbs"

	OSWindows = "windows"
)

// GetFile get the config file from base dir
func GetFile(filename string) string {
	if runtime.GOOS == OSWindows {
		return filename
	}

	return filepath.Join(BaseDir, filename)
}

// GetRunFile get the runtime file from run dir
func GetRunFile(dir, filename string) string {
	return filepath.Join(dir, RunDir, filename)
}

// MatchWay define match way
type MatchWay string

// define match way
const (
	MatchPrefix  MatchWay = "match_prefix"
	MatchSuffix  MatchWay = "match_suffix"
	MatchEqual   MatchWay = "match_equal"
	MatchInclude MatchWay = "match_include"
)

// CmdReplaceRule define replace rules for input cmd
type CmdReplaceRule struct {
	Cmd    string   `json:"cmd" value:"" usage:"used to match target cmd, it's optional"`
	CmdWay MatchWay `json:"cmd_match_way" value:"match_equal" usage:"match way for cmd"`

	Anchor    string   `json:"anchor_key" value:"" usage:"anchor key to search, is can't be empty"`
	AnchorWay MatchWay `json:"anchor_match_way" value:"match_equal" usage:"match way for anchor"`

	ReplaceOffset int    `json:"replace_offset" value:"0" usage:"the offset with Anchor index, it can't out of range of parameter array"`
	SourceRegKey  string `json:"source_reg_key" value:"" usage:"the source key to be replaced specified by regex"`
	TargetKey     string `json:"target_key" value:"" usage:"the target key to be replaced"`
}

// Replace replace input parameters with this rule
func (r CmdReplaceRule) Replace(cmd string, parameters []string) {
	paramlen := len(parameters)
	if paramlen == 0 {
		// fmt.Printf("input parameter is empty")
		return
	}

	if cmd != "" && r.Cmd != "" && !match(r.Cmd, cmd, r.CmdWay) {
		// fmt.Printf("input cmd[%s] not satisfied with rule cmd[%s]", cmd, r.Cmd)
		return
	}

	for i := range parameters {
		if match(r.Anchor, parameters[i], r.AnchorWay) {
			targetindex := i + r.ReplaceOffset
			if targetindex < 0 || targetindex >= paramlen {
				continue
			}

			re, err := regexp.Compile(r.SourceRegKey)
			if err == nil {
				parameters[targetindex] = re.ReplaceAllString(parameters[targetindex], r.TargetKey)
			}
		}
	}
}

func match(key, src string, way MatchWay) bool {
	if key == "" && src == "" {
		return true
	}

	switch way {
	case MatchEqual:
		return src == key
	case MatchPrefix:
		return strings.HasPrefix(src, key)
	case MatchSuffix:
		return strings.HasSuffix(src, key)
	case MatchInclude:
		return strings.Index(src, key) != -1
	default:
		return false
	}
}
