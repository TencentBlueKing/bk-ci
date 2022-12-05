/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package ue4

import (
	"fmt"
	"os"
	"strings"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/astc"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/cc"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/cl"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/clfilter"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/lib"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/link"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/shader"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

const (
	defaultCLCompiler                 = "cl.exe"
	defaultShaderCompiler             = "ShaderCompileWorker.exe"
	defaultShaderCompilerMac          = "ShaderCompileWorker"
	defaultClangCompiler              = "clang.exe"
	defaultClangPlusPlusCompiler      = "clang++.exe"
	defaultClangLinuxCompiler         = "clang"
	defaultClangPlusPlusLinuxCompiler = "clang++"
	defaultLibCompiler                = "lib.exe"
	defaultLinkCompiler               = "link.exe"
	defaultCLFilterCompiler           = "cl-filter.exe"
	defaultAstcsse2Compiler           = "astcenc-sse2.exe"
	defaultAstcCompiler               = "astcenc.exe"

	hookConfigPathDefault  = "bk_default_rules.json"
	hookConfigPathCCCommon = "bk_cl_rules.json"
)

// NewUE4 get ue4 scene handler
func NewUE4() (handler.Handler, error) {
	return &UE4{
		sandbox: &dcSyscall.Sandbox{},
	}, nil
}

// UE4 定义了ue4场景下的各类子场景总和,
// 包含了win/mac/linux平台下的c/c++代码编译, 链接, shader编译等
type UE4 struct {
	sandbox      *dcSyscall.Sandbox
	innerhandler handler.Handler
}

// InitSandbox set sandbox to ue4 scene handler
func (u *UE4) InitSandbox(sandbox *dcSyscall.Sandbox) {
	u.sandbox = sandbox
	if u.innerhandler != nil {
		u.innerhandler.InitSandbox(sandbox)
	}
}

// InitExtra no need
func (u *UE4) InitExtra(extra []byte) {
}

// ResultExtra no need
func (u *UE4) ResultExtra() []byte {
	return nil
}

// RenderArgs no need change
func (u *UE4) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// PreWork no need
func (u *UE4) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

// PostWork no need
func (u *UE4) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// GetPreloadConfig get preload config
func (u *UE4) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return getPreloadConfig(u.getPreLoadConfigPath(config))
}

func getPreloadConfig(configPath string) (*dcSDK.PreloadConfig, error) {
	f, err := os.Open(configPath)
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = f.Close()
	}()

	var pConfig dcSDK.PreloadConfig
	if err = codec.DecJSONReader(f, &pConfig); err != nil {
		return nil, err
	}

	return &pConfig, nil
}

func (u *UE4) getPreLoadConfigPath(config dcType.BoosterConfig) string {
	if config.Works.HookConfigPath != "" {
		return config.Works.HookConfigPath
	}

	// degrade will not contain the UE4
	if config.Works.Degraded {
		return dcConfig.GetFile(hookConfigPathDefault)
	}

	return dcConfig.GetFile(hookConfigPathCCCommon)
}

// GetFilterRules will return filter rule to booster
func (u *UE4) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return []dcSDK.FilterRuleItem{
		{
			Rule:       dcSDK.FilterRuleFileSuffix,
			Operator:   dcSDK.FilterRuleOperatorEqual,
			Standard:   ".pch",
			HandleType: dcSDK.FilterRuleHandleAllDistribution,
		},
		{
			Rule:       dcSDK.FilterRuleFileSuffix,
			Operator:   dcSDK.FilterRuleOperatorEqual,
			Standard:   ".gch",
			HandleType: dcSDK.FilterRuleHandleAllDistribution,
		},
	}, nil
}

// PreExecuteNeedLock 防止预处理跑满本机CPU
func (u *UE4) PreExecuteNeedLock(command []string) bool {
	return true
}

// PostExecuteNeedLock 防止回传的文件读写跑满本机磁盘
func (u *UE4) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return true
}

// PreLockWeight decide pre-execute lock weight, default 1
func (u *UE4) PreLockWeight(command []string) int32 {
	if u.innerhandler == nil {
		u.initInnerHandle(command)
	}
	if u.innerhandler != nil {
		if u.sandbox != nil {
			u.innerhandler.InitSandbox(u.sandbox.Fork())
		}
		return u.innerhandler.PreLockWeight(command)
	}
	return 1
}

// PreExecute 预处理, 根据不同的command来确定不同的子场景
func (u *UE4) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	if command == nil || len(command) == 0 {
		return nil, fmt.Errorf("command is nil")
	}

	u.initInnerHandle(command)

	if u.innerhandler != nil {
		if u.sandbox != nil {
			u.innerhandler.InitSandbox(u.sandbox.Fork())
		}
		return u.innerhandler.PreExecute(command)
	}

	return nil, fmt.Errorf("not support for command %s", command[0])
}

// PreExecute 预处理, 根据不同的command来确定不同的子场景
func (u *UE4) initInnerHandle(command []string) {
	if command == nil || len(command) == 0 {
		return
	}

	if u.innerhandler == nil {
		if strings.HasSuffix(command[0], defaultCLCompiler) {
			u.innerhandler = cl.NewTaskCL()
			blog.Debugf("ue4: innerhandle with cl for command[%s]", command[0])
		} else if strings.HasSuffix(command[0], defaultCLFilterCompiler) {
			u.innerhandler = clfilter.NewTaskCLFilter()
			blog.Debugf("ue4: innerhandle with clfilter for command[%s]", command[0])
		} else if strings.HasSuffix(command[0], defaultShaderCompiler) ||
			strings.HasSuffix(command[0], defaultShaderCompilerMac) {
			u.innerhandler = shader.NewUE4Shader()
			blog.Debugf("ue4: innerhandle with shader for command[%s]", command[0])
		} else if strings.HasSuffix(command[0], defaultClangCompiler) ||
			strings.HasSuffix(command[0], defaultClangPlusPlusCompiler) ||
			strings.HasSuffix(command[0], defaultClangLinuxCompiler) ||
			strings.HasSuffix(command[0], defaultClangPlusPlusLinuxCompiler) {
			u.innerhandler = cc.NewTaskCC()
			blog.Debugf("ue4: innerhandle with cc for command[%s]", command[0])
		} else if strings.HasSuffix(command[0], defaultAstcsse2Compiler) ||
			strings.HasSuffix(command[0], defaultAstcCompiler) {
			u.innerhandler = astc.NewTextureCompressor()
			blog.Debugf("ue4: innerhandle with clfilter for command[%s]", command[0])
		} else {
			if env.GetEnv(env.KeyExecutorSupportLink) == "true" {
				if strings.HasSuffix(command[0], defaultLibCompiler) {
					u.innerhandler = lib.NewTaskLib()
					blog.Debugf("ue4: innerhandle with lib for command[%s]", command[0])
				} else if strings.HasSuffix(command[0], defaultLinkCompiler) {
					u.innerhandler = link.NewTaskLink()
					blog.Debugf("ue4: innerhandle with link for command[%s]", command[0])
				}
			}
		}
	}
}

// NeedRemoteResource check whether this command need remote resource
func (u *UE4) NeedRemoteResource(command []string) bool {
	if u.innerhandler != nil {
		return u.innerhandler.NeedRemoteResource(command)
	}

	u.initInnerHandle(command)

	if u.innerhandler != nil {
		return u.innerhandler.NeedRemoteResource(command)
	}

	return false
}

// RemoteRetryTimes will return the remote retry times
func (u *UE4) RemoteRetryTimes() int {
	if u.innerhandler != nil {
		return u.innerhandler.RemoteRetryTimes()
	}

	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (u *UE4) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	if u.innerhandler != nil {
		return u.innerhandler.PostLockWeight(result)
	}

	return 1
}

// PostExecute 后置处理
func (u *UE4) PostExecute(r *dcSDK.BKDistResult) error {
	if u.innerhandler != nil {
		return u.innerhandler.PostExecute(r)
	}

	return fmt.Errorf("not support")
}

// LocalExecuteNeed no need
func (u *UE4) LocalExecuteNeed(command []string) bool {
	if u.innerhandler != nil {
		return u.innerhandler.LocalExecuteNeed(command)
	}

	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (u *UE4) LocalLockWeight(command []string) int32 {
	if u.innerhandler == nil {
		u.initInnerHandle(command)
	}

	if u.innerhandler != nil {
		if u.sandbox != nil {
			u.innerhandler.InitSandbox(u.sandbox.Fork())
		}
		return u.innerhandler.LocalLockWeight(command)
	}

	return 1
}

// LocalExecute no need
func (u *UE4) LocalExecute(command []string) (int, error) {
	if u.innerhandler != nil {
		return u.innerhandler.LocalExecute(command)
	}

	return 0, nil
}

// FinalExecute 清理临时文件
func (u *UE4) FinalExecute(args []string) {
	if u.innerhandler != nil {
		u.innerhandler.FinalExecute(args)
	}
}
