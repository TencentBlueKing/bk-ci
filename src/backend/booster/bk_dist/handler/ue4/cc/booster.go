/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cc

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

const (
	hookConfigPathDefault     = "bk_default_rules.json"
	hookConfigPathCCCommon    = "bk_cc_rules.json"
	hookConfigPathCCOnlyCache = "bk_cc_only_cache_rules.json"
	hookConfigPathCCAndCache  = "bk_cc_and_cache_rules.json"
)

// ProjectExtraData describe the extra data store in project
type ProjectExtraData struct {
	CCacheEnable bool `json:"ccache_enable"`
}

// InitExtra receive disttask custom extra data and initialize its settings
func (cc *TaskCC) InitExtra(extra []byte) {
	var data disttask.CustomData
	if err := codec.DecJSON(extra, &data); err != nil {
		blog.Errorf("booster: init extra data for handler failed: %v", err)
		return
	}

	var projectData ProjectExtraData
	if err := codec.DecJSON([]byte(data.ExtraProjectData), &projectData); err != nil {
		blog.Errorf("booster: init extra data for handler failed: %v", err)
	}

	cc.ccacheEnable = projectData.CCacheEnable
	blog.Infof("booster: cc handler ccache enable: %t", cc.ccacheEnable)
}

// ResultExtra no need
func (cc *TaskCC) ResultExtra() []byte {
	return nil
}

// RenderArgs receive the user's origin commands, and render some extra thins to it
// For instance: bazel command should be add extra --action_env
func (cc *TaskCC) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	if !config.Works.Bazel {
		return originArgs
	}

	additions := make([]string, 0, 10)
	for k, v := range config.Works.Environments {
		v = strings.ReplaceAll(v, "\"", "\\\"")
		v = strings.ReplaceAll(v, "$", "\\$")
		additions = append(additions, fmt.Sprintf("--action_env=%s=\"%s\"", k, v))
	}

	originArgs += " " + strings.Join(additions, " ")
	return originArgs
}

// PreWork 处理整个编译的前置工作, 例如清除ccache数据缓存, 更新launcher脚本等
func (cc *TaskCC) PreWork(config *dcType.BoosterConfig) error {
	if cc.ccacheEnable {
		sandbox := cc.sandbox.Fork()
		if _, err := sandbox.ExecCommand("/bin/bash", "-c", "ccache -z"); err != nil {
			blog.Errorf("booster: exec command ccache -z error: %v", err)
		}
	}

	return nil
}

// PostWork 处理整个编译的后置工作, 收集ccache数据
func (cc *TaskCC) PostWork(config *dcType.BoosterConfig) error {
	if cc.ccacheEnable {
		if _, err := cc.statisticsCCache(); err != nil {
			blog.Errorf("booster: get ccache statics failed: %v", err)
		}
	}

	return nil
}

// GetPreloadConfig 获取preload配置
func (cc *TaskCC) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return getPreloadConfig(cc.getPreLoadConfigPath(config))
}

func (cc *TaskCC) getPreLoadConfigPath(config dcType.BoosterConfig) string {
	if config.Works.HookConfigPath != "" {
		return config.Works.HookConfigPath
	}

	// degrade will not contain the CC
	if config.Works.Degraded {
		if cc.ccacheEnable {
			return dcConfig.GetFile(hookConfigPathCCOnlyCache)
		}
		return dcConfig.GetFile(hookConfigPathDefault)
	}

	if cc.ccacheEnable {
		return dcConfig.GetFile(hookConfigPathCCAndCache)
	}
	return dcConfig.GetFile(hookConfigPathCCCommon)
}

func (cc *TaskCC) statisticsCCache() (*types.Ccache, error) {
	sandbox := cc.sandbox.Fork()
	buf := bytes.NewBuffer(make([]byte, 1024))
	sandbox.Stdout = buf

	if _, err := sandbox.ExecCommand("/bin/bash", "-c", "ccache -s"); err != nil {
		return nil, err
	}

	ccache := &types.Ccache{}
	arr := strings.Split(buf.String(), "\n")
	for _, str := range arr {
		str = strings.TrimSpace(str)
		if str == "" {
			continue
		}
		kv := strings.Split(str, "  ")
		if len(kv) < 2 {
			continue
		}
		key := strings.TrimSpace(kv[0])
		value := strings.TrimSpace(kv[len(kv)-1])
		switch key {
		case "cache directory":
			ccache.CacheDir = value
		case "primary config":
			ccache.PrimaryConfig = value
		case "secondary config":
			ccache.SecondaryConfig = value
		case "cache hit (direct)":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.DirectHit = i
		case "cache hit (preprocessed)":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.PreprocessedHit = i
		case "cache miss":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.CacheMiss = i
		case "files in cache":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.FilesInCache = i
		case "cache size":
			ccache.CacheSize = value
		case "max cache size":
			ccache.MaxCacheSize = value
		}
	}

	return ccache, nil
}
