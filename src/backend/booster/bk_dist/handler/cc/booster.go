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
	"os"
	"runtime"
	"strings"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"

	cp "github.com/otiai10/copy"
)

const (
	hookConfigPathDefault     = "bk_default_rules.json"
	hookConfigPathCCCommon    = "bk_cc_rules.json"
	hookConfigPathCCOnlyCache = "bk_cc_only_cache_rules.json"
	hookConfigPathCCAndCache  = "bk_cc_and_cache_rules.json"
	hookConfigPathCCLauncher  = "bk_cc_launcher_rules.json"

	launcherTemplateDir = "/etc/bk_dist/launcher"
	launcherFile        = "launcher"
	launcherMark        = "$LAUNCHER_COMMAND"
	launcherHookMark    = "$LAUNCHER_HOOK"

	envCCacheNoCPP2    = "CCACHE_NOCPP2"
	envCCachePrefix    = "CCACHE_PREFIX"
	envCCachePrefixCPP = "CCACHE_PREFIX_CPP"
	envPATH            = "PATH"

	executorName = "bk-dist-executor"
	ccacheName   = "ccache"
)

var bazelActionConstOptions = map[string]bool{
	// env.KeyExecutorHookPreloadLibraryLinux:             true,
	// env.KeyExecutorHookPreloadLibraryMacos:             true,
	env.GetEnvKey(env.KeyExecutorHookConfigContent):    true,
	env.GetEnvKey(env.KeyExecutorHookConfigContentRaw): true,
	env.GetEnvKey(env.BoosterType):                     true,
	env.GetEnvKey(env.KeyExecutorIOTimeout):            true,
}

func appendPreload() error {
	switch runtime.GOOS {
	case "linux":
		bazelActionConstOptions[env.KeyExecutorHookPreloadLibraryLinux] = true
	case "darwin":
		bazelActionConstOptions[env.KeyExecutorHookPreloadLibraryMacos] = true
	default:
		blog.Warnf("booster: What os is this?", runtime.GOOS)
	}

	return nil
}

// ProjectExtraData describe the extra data store in project
// ccache_enable and ccache_enabled are both to control ccache usage, if one of them is true, then ccache enabled.
type ProjectExtraData struct {
	CCacheEnable  bool `json:"ccache_enable"`
	CCacheEnabled bool `json:"ccache_enabled"`
}

// InitExtra receive disttask custom extra data and initialize its settings
func (cc *TaskCC) InitExtra(extra []byte) {
	blog.Infof("booster: init extra got data: %s", string(extra))

	var data disttask.CustomData
	if err := codec.DecJSON(extra, &data); err != nil {
		blog.Warnf("booster: init extra data for handler failed: %v", err)
		return
	}

	var projectData ProjectExtraData
	if err := codec.DecJSON([]byte(data.ExtraProjectData), &projectData); err != nil {
		blog.Warnf("booster: init extra data for handler failed: %v", err)
	}

	// 兼容不同版本的协议
	cc.ccacheEnable = projectData.CCacheEnable || projectData.CCacheEnabled
	blog.Infof("booster: cc handler ccache enable: %t", cc.ccacheEnable)
}

// ResultExtra generate the extra result data to upload stats
func (cc *TaskCC) ResultExtra() []byte {
	var message disttask.Message

	message.Type = disttask.MessageTypeRecordStats
	message.MessageRecordStats.CCacheStats.CacheDir = cc.ccacheStats.CacheDir
	message.MessageRecordStats.CCacheStats.PrimaryConfig = cc.ccacheStats.PrimaryConfig
	message.MessageRecordStats.CCacheStats.SecondaryConfig = cc.ccacheStats.SecondaryConfig
	message.MessageRecordStats.CCacheStats.DirectHit = cc.ccacheStats.DirectHit
	message.MessageRecordStats.CCacheStats.PreprocessedHit = cc.ccacheStats.PreprocessedHit
	message.MessageRecordStats.CCacheStats.CacheMiss = cc.ccacheStats.CacheMiss
	message.MessageRecordStats.CCacheStats.CalledForLink = cc.ccacheStats.CalledForLink
	message.MessageRecordStats.CCacheStats.CalledForPreProcessing = cc.ccacheStats.CalledForPreProcessing
	message.MessageRecordStats.CCacheStats.UnsupportedSourceLanguage = cc.ccacheStats.UnsupportedSourceLanguage
	message.MessageRecordStats.CCacheStats.NoInputFile = cc.ccacheStats.NoInputFile
	message.MessageRecordStats.CCacheStats.FilesInCache = cc.ccacheStats.FilesInCache
	message.MessageRecordStats.CCacheStats.CacheSize = cc.ccacheStats.CacheSize
	message.MessageRecordStats.CCacheStats.MaxCacheSize = cc.ccacheStats.MaxCacheSize

	var data []byte
	_ = codec.EncJSON(message, &data)
	return data
}

// RenderArgs receive the user's origin commands, and render some extra thins to it
// For instance: bazel command should be add extra --action_env
func (cc *TaskCC) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	// old bazel actions, no recommend
	if config.Works.Bazel {
		additions := make([]string, 0, 10)
		for k, v := range config.Works.Environments {
			additions = append(additions, wrapActionOptions("action_env", k, v))
		}

		originArgs += " " + strings.Join(additions, " ")
		return originArgs
	}

	appendPreload()

	if config.Works.BazelPlus || config.Works.Bazel4Plus {
		additions := make([]string, 0, 10)
		for k, v := range config.Works.Environments {
			if _, ok := bazelActionConstOptions[k]; !ok {
				continue
			}

			additions = append(additions, wrapActionOptions("action_env", k, v))
			if config.Works.Bazel4Plus {
				additions = append(additions, wrapActionOptions("host_action_env", k, v))
			}
		}

		if len(additions) > 0 {
			originArgs += " " + strings.Join(additions, " ")
		}
		return originArgs
	}

	return originArgs
}

// PreWork 处理整个编译的前置工作, 例如清除ccache数据缓存, 更新launcher脚本等
func (cc *TaskCC) PreWork(config *dcType.BoosterConfig) error {
	realExecutor, err := dcUtil.CheckExecutable(executorName)
	if err != nil {
		blog.Errorf("booster: could not find executor: %s", executorName)
		return err
	}

	sandbox := cc.sandbox.Fork()
	var buf bytes.Buffer
	sandbox.Stdout = &buf
	if _, err := sandbox.ExecScripts("ccache -z"); err != nil {
		blog.Warnf("booster: run ccache -z error: %v", err)
	}

	if cc.ccacheEnable {
		config.Works.Environments[envCCacheNoCPP2] = "1"
		config.Works.Environments[envCCachePrefix] = realExecutor
		config.Works.Environments[envCCachePrefixCPP] = realExecutor
	}

	if config.Works.Launcher {
		profile := dcConfig.GetRunFile(config.Works.RunDir, "tbs_env.profile")
		config.Works.OutputEnvSourceFile = append(config.Works.OutputEnvSourceFile, profile)
		config.Works.Environments[env.GetEnvKey(env.KeyExecutorEnvProfile)] = profile

		targetDir := dcConfig.GetRunFile(config.Works.RunDir, "")
		if err := cp.Copy(launcherTemplateDir+"/", targetDir); err != nil {
			blog.Warnf("booster: copy launcher files from %s to %s failed: %v",
				launcherTemplateDir, targetDir, err)
		}

		launcher := realExecutor
		if cc.ccacheEnable {
			launcher = fmt.Sprintf("%s=%s %s=%s %s",
				envCCachePrefix, realExecutor, envCCachePrefixCPP, realExecutor, ccacheName)
		}

		target := dcConfig.GetRunFile(config.Works.RunDir, launcherFile)
		if err := replaceFileContent(target, launcherMark, launcher); err != nil {
			blog.Warnf("booster: replace launcher file content in %s failed: %v", target, err)
		}
	}

	if config.Works.BazelPlus || config.Works.Bazel4Plus {
		source := dcConfig.GetFile(hookConfigPathCCLauncher)
		if config.Works.HookConfigPath != "" {
			source = config.Works.HookConfigPath
		}

		target := dcConfig.GetRunFile(config.Works.RunDir, hookConfigPathCCLauncher)

		if err := cp.Copy(source, target); err != nil {
			blog.Warnf("booster: copy launcher hook config from %s to %s failed: %v", source, target, err)
		}

		if err := replaceFileContent(
			target, launcherHookMark, dcConfig.GetRunFile(config.Works.RunDir, launcherFile)); err != nil {
			blog.Warnf("booster: replace launcher file content in %s failed: %v", target, err)
		}

		config.Works.HookConfigPath = target
	}

	// remove cache symlink to gcc from PATH
	_ = os.Setenv(envPATH, removeCCacheBinFromPATH(os.Getenv(envPATH)))

	return nil
}

// PostWork 处理整个编译的后置工作, 收集ccache数据
func (cc *TaskCC) PostWork(config *dcType.BoosterConfig) error {
	ccacheStats, err := cc.statisticsCCache()
	if err != nil {
		blog.Warnf("booster: get ccache statics failed: %v", err)
		return nil
	}
	cc.ccacheStats = *ccacheStats

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
		return dcConfig.GetFile(hookConfigPathCCOnlyCache)
	}
	return dcConfig.GetFile(hookConfigPathCCCommon)
}
