//go:build !loong64 && !darwin

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package config

import (
	"bytes"
	"strings"

	"github.com/jaypipes/ghw"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

// GetCpuAndGpuInfo 获取 CPU 和 GPU 型号信息
func GetCpuAndGpuInfo() (cpuInfo string, gpuInfo string) {
	cpuInfo = getCPUProductInfo()
	gpuInfo = getGPUProductInfo()
	logs.Infof("cpu: %s, gpu: %s", cpuInfo, gpuInfo)
	return cpuInfo, gpuInfo
}

func getCPUProductInfo() (cpuInfo string) {
	defer func() {
		if r := recover(); r != nil {
			logs.Warnf("get cpu info panic, skip cpu detection: %v", r)
			cpuInfo = ""
		}
	}()

	cpu, err := ghw.CPU()
	if err != nil {
		logs.WithError(err).Warn("get cpu info error, skip cpu detection")
		return ""
	}
	if cpu == nil {
		logs.Warn("get cpu info empty result, skip cpu detection")
		return ""
	}

	cpuInfoBuf := bytes.Buffer{}
	for _, c := range cpu.Processors {
		if c == nil || c.Model == "" {
			continue
		}
		cpuInfoBuf.WriteString(c.Model)
		cpuInfoBuf.WriteString(";")
	}
	return strings.TrimSuffix(cpuInfoBuf.String(), ";")
}

func getGPUProductInfo() (gpuInfo string) {
	defer func() {
		if r := recover(); r != nil {
			logs.Warnf("get gpu info panic, skip gpu detection: %v", r)
			gpuInfo = ""
		}
	}()

	gpu, err := ghw.GPU()
	if err != nil {
		logs.WithError(err).Warn("get gpu info error, skip gpu detection")
		return ""
	}
	if gpu == nil {
		logs.Warn("get gpu info empty result, skip gpu detection")
		return ""
	}

	gpuInfoBuf := bytes.Buffer{}
	for _, card := range gpu.GraphicsCards {
		if card == nil || card.DeviceInfo == nil || card.DeviceInfo.Product == nil || card.DeviceInfo.Product.Name == "" {
			continue
		}
		gpuInfoBuf.WriteString(card.DeviceInfo.Product.Name)
		gpuInfoBuf.WriteString(";")
	}
	return strings.TrimSuffix(gpuInfoBuf.String(), ";")
}
