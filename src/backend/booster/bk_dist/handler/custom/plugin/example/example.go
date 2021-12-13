/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package main

import (
	"fmt"
	"os"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

const (
	hookConfigPath = "bk_custom_example_rules.json"
)

// New get a example handler
func New() handler.Handler {
	return &Example{}
}

// Example 作为go-plugin的handler实现样例
// 实现了一个将原命令完整放到远程执行的handler
type Example struct {
	sandbox *dcSyscall.Sandbox
}

// InitExtra 无需处理
func (c *Example) InitExtra([]byte) {
}

// ResultExtra 无需处理
func (c *Example) ResultExtra() []byte {
	return nil
}

// InitSandbox 初始化执行环境sandbox
func (c *Example) InitSandbox(sandbox *dcSyscall.Sandbox) {
	c.sandbox = sandbox
}

// PreWork 无需处理
func (c *Example) PreWork(*dcType.BoosterConfig) error {
	return nil
}

// PostWork 无需处理
func (c *Example) PostWork(*dcType.BoosterConfig) error {
	return nil
}

// RenderArgs 执行原本命令
func (c *Example) RenderArgs(_ dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// GetPreloadConfig 从指定路径获取hook配置, 在测试执行时需要预先创建这个文件和配置
func (c *Example) GetPreloadConfig(dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return getPreloadConfig(dcConfig.GetFile(hookConfigPath))
}

// GetFilterRules 无需处理
func (c *Example) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return nil, nil
}

// PreExecute 前置处理, 获得单条命令, 组装其将要在远程执行的内容
// 在这里我们原封不动地将原命令放入BKCommand, 让其在远程执行
func (c *Example) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	if len(command) < 1 {
		return nil, fmt.Errorf("invalid command and pararms")
	}

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{{
			ExeName: command[0],
			Params:  command[1:],
		}},
	}, nil
}

// PreExecuteNeedLock 无需获取pre-lock
func (c *Example) PreExecuteNeedLock([]string) bool {
	return false
}

// LocalExecuteNeed 无需自定义本地执行
func (c *Example) LocalExecuteNeed([]string) bool {
	return false
}

// LocalExecute 无需自定义本地执行
func (c *Example) LocalExecute([]string) (int, error) {
	return 0, nil
}

// PostExecute 获取命令在远程执行的结果, 并判断是否成功, 若远程任务返回码为0, 则成功执行, 否则失败
// 也可以考虑将Results中的远程日志打印到本地, 获得执行结果
func (c *Example) PostExecute(result *dcSDK.BKDistResult) error {
	if result != nil && len(result.Results) > 0 && result.Results[0].RetCode == 0 {

		return nil
	}

	return fmt.Errorf("remote execute error")
}

// PostExecuteNeedLock 无需获取post-lock
func (c *Example) PostExecuteNeedLock(*dcSDK.BKDistResult) bool {
	return false
}

// FinalExecute 无需处理
func (c *Example) FinalExecute([]string) {
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
