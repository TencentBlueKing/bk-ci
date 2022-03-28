/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package sdk

import (
	"fmt"
	"strings"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// type ToolChainKey string

// const (
// 	ToolChainKeyUE4Shader  ToolChainKey = "toolchain_key_ue4_shader"
// 	ToolChainKeyUE4Compile ToolChainKey = "toolchain_key_ue4_compile"
// )

// func (t ToolChainKey) String() string {
// 	return string(t)
// }

// GetJsonToolChainKey get key from original full exe path, fit json format
func GetJsonToolChainKey(origiralfullexepath string) string {
	return strings.Replace(origiralfullexepath, "\\", "/", -1)
}

// ToolFile describe tool file target
type ToolFile struct {
	LocalFullPath      string `json:"local_full_path"`
	RemoteRelativePath string `json:"remote_relative_path"`
}

// OneToolChain describe the single tool chain info
type OneToolChain struct {
	ToolKey                string     `json:"tool_key"`
	ToolName               string     `json:"tool_name"`
	ToolLocalFullPath      string     `json:"tool_local_full_path"`
	ToolRemoteRelativePath string     `json:"tool_remote_relative_path"`
	Files                  []ToolFile `json:"files"`
}

// Toolchain describe the toolchains
type Toolchain struct {
	Toolchains []OneToolChain `json:"toolchains"`
}

// GetToolchainEnvValue get the generated env value
func (t *Toolchain) GetToolchainEnvValue() (string, error) {
	if t == nil {
		return "", fmt.Errorf("tool chain is nil")
	}

	envvalue := ""
	isfirst := true
	for _, v := range t.Toolchains {
		if !isfirst {
			envvalue += ";"
		}

		envvalue += fmt.Sprintf("%s|%s", v.ToolName, v.ToolRemoteRelativePath)

		if isfirst {
			isfirst = false
		}
	}

	return envvalue, nil
}

// ResolveToolchainEnvValue receive generated env value and return the k-v map
func ResolveToolchainEnvValue(value string) (map[string]string, error) {
	if value == "" {
		return nil, fmt.Errorf("env value is empty")
	}

	outmap := map[string]string{}
	envs := strings.Split(value, ";")
	for _, v := range envs {
		kv := strings.Split(v, "|")
		if len(kv) == 2 {
			outmap[kv[0]] = kv[1]
		}
	}
	return outmap, nil
}

// ToFileDesc parse toolchains to file targets
func (t *Toolchain) ToFileDesc() ([]FileDesc, error) {
	if t == nil {
		return nil, fmt.Errorf("tool chain is nil")
	}

	toolfiles := make([]FileDesc, 0, 0)
	for _, v := range t.Toolchains {
		existed, fileSize, modifyTime, fileMode := dcFile.Stat(v.ToolLocalFullPath).Batch()
		if !existed {
			err := fmt.Errorf("tool chain file %s not existed", v.ToolLocalFullPath)
			blog.Errorf("%v", err)
			return nil, err
		}

		toolfiles = append(toolfiles, FileDesc{
			FilePath:           v.ToolLocalFullPath,
			Compresstype:       protocol.CompressLZ4,
			FileSize:           fileSize,
			Lastmodifytime:     modifyTime,
			Md5:                "",
			Targetrelativepath: v.ToolRemoteRelativePath,
			Filemode:           fileMode,
		})

		for _, f := range v.Files {
			existed, fileSize, modifyTime, fileMode = dcFile.Stat(f.LocalFullPath).Batch()
			if !existed {
				err := fmt.Errorf("tool chain file %s not existed", f.LocalFullPath)
				blog.Errorf("%v", err)
				return nil, err
			}

			toolfiles = append(toolfiles, FileDesc{
				FilePath:           f.LocalFullPath,
				Compresstype:       protocol.CompressLZ4,
				FileSize:           fileSize,
				Lastmodifytime:     modifyTime,
				Md5:                "",
				Targetrelativepath: f.RemoteRelativePath,
				Filemode:           fileMode,
			})
		}
	}

	return toolfiles, nil
}
