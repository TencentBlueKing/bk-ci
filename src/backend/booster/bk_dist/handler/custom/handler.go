/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package custom

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/custom/plugin"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

type Type int

const (
	TypeUnknown Type = iota
	TypePlugin
	TypeSharedLibrary
	TypeScript
	TypeAPI
)

// String return string of Custom Type
func (t Type) String() string {
	name, ok := typeName[t]
	if ok {
		return name
	}

	return "unknown"
}

var typeName = map[Type]string{
	TypeUnknown:       "unknown",
	TypePlugin:        "plugin",
	TypeSharedLibrary: "shared-library",
	TypeScript:        "script",
	TypeAPI:           "api",
}

// Settings 定义了custom配置, 决定使用哪种加载方式, 以及目标库的位置
type Settings struct {
	T    Type   `json:"type"`
	Path string `json:"path"`
}

// NewCustom get a new custom handler
func NewCustom() (handler.Handler, error) {
	var c handler.Handler
	var err error
	var s Settings

	_ = codec.DecJSON([]byte(env.GetEnv(env.KeyCustomSetting)), &s)

	switch s.T {
	case TypePlugin:
		c, err = plugin.New(s.Path)
	case TypeSharedLibrary:
	case TypeScript:
	case TypeAPI:
	default:
		err = fmt.Errorf("unknown custom type: %d", s.T)
	}

	if err != nil {
		return nil, err
	}

	return &Custom{
		sandbox:      &dcSyscall.Sandbox{},
		innerHandler: c,
	}, nil
}

// Custom 定义一个总的custom handler, 转发actions到具体的实现上
type Custom struct {
	sandbox      *dcSyscall.Sandbox
	innerHandler handler.Handler
}

// InitExtra 用来解析从project拿到的extra信息, 详情参见: disttask.CustomData
func (c *Custom) InitExtra(extra []byte) {
	c.innerHandler.InitExtra(extra)
}

// ResultExtra 用来生成在结束后需要上传的extra信息
func (c *Custom) ResultExtra() []byte {
	return nil
}

// InitSandbox 在执行每个具体任务之前, 都会传入一个当前的执行环境sandbox
func (c *Custom) InitSandbox(sandbox *dcSyscall.Sandbox) {
	c.innerHandler.InitSandbox(sandbox)
}

// PreWork 处理整个任务的前置工作, 如工作空间初始化
func (c *Custom) PreWork(config *dcType.BoosterConfig) error {
	return c.innerHandler.PreWork(config)
}

// PostWork 处理整个任务的后置工作, 如缓存命中率统计
func (c *Custom) PostWork(config *dcType.BoosterConfig) error {
	return c.innerHandler.PostWork(config)
}

// RenderArgs 在执行整个任务的指令之前, 会传入原始任务, 允许handler修改
func (c *Custom) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return c.innerHandler.RenderArgs(config, originArgs)
}

// GetPreloadConfig 获取preload配置, 用来决定hook的对象和处理方法
func (c *Custom) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return c.innerHandler.GetPreloadConfig(config)
}

// GetFilterRules 获取filter rules配置, 用来决定文件分发的策略
func (c *Custom) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return c.innerHandler.GetFilterRules()
}

// PreLockWeight decide pre-execute lock weight, default 1
func (c *Custom) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute 单个任务的预处理, 如c/c++编译的pre-process, 决定了分发到远程处理的任务信息
func (c *Custom) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	return c.innerHandler.PreExecute(command)
}

// PreExecuteNeedLock 决定是否需要在执行PreExecute之前获取一个pre-lock
func (c *Custom) PreExecuteNeedLock(command []string) bool {
	return c.innerHandler.PreExecuteNeedLock(command)
}

// LocalExecuteNeed 决定是否要自定义本地执行的内容
func (c *Custom) LocalExecuteNeed(command []string) bool {
	return c.innerHandler.LocalExecuteNeed(command)
}

// LocalLockWeight decide local-execute lock weight, default 1
func (c *Custom) LocalLockWeight(command []string) int32 {
	return 1
}

// LocalExecute 自定义本地执行
func (c *Custom) LocalExecute(command []string) (int, error) {
	return c.innerHandler.LocalExecute(command)
}

// NeedRemoteResource check whether this command need remote resource
func (c *Custom) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (c *Custom) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (c *Custom) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute 单个任务的后置处理, 需要处理远程任务执行的结果
func (c *Custom) PostExecute(result *dcSDK.BKDistResult) error {
	return c.innerHandler.PostExecute(result)
}

// PostExecuteNeedLock 决定是否需要在执行PostExecute之前获取一个post-lock
func (c *Custom) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return c.innerHandler.PostExecuteNeedLock(result)
}

// FinalExecute 收尾工作, 无论如何都会执行的步骤, 例如清理临时文件
func (c *Custom) FinalExecute(command []string) {
	c.innerHandler.FinalExecute(command)
}
