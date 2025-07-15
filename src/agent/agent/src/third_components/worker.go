package third_components

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"sync"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/sirupsen/logrus"
)

var Worker *WorkerType

type WorkerType struct {
	version string
	lock    sync.RWMutex
	logs    *logrus.Entry
}

func (w *WorkerType) SetVersion(version string) {
	w.lock.Lock()
	defer w.lock.Unlock()
	w.version = version
}

func (w *WorkerType) GetVersion() string {
	w.lock.RLock()
	defer w.lock.RUnlock()
	return w.version
}

// DetectWorkerVersion 检查worker版本
func (w *WorkerType) DetectWorkerVersion() string {
	return w.DetectWorkerVersionByDir(systemutil.GetWorkDir())
}

// DetectWorkerVersionByDir 检测指定目录下的Worker文件版本
func (w *WorkerType) DetectWorkerVersionByDir(workDir string) string {
	jar := fmt.Sprintf("%s/%s", workDir, config.WorkAgentFile)
	tmpDir, _ := systemutil.MkBuildTmpDir()
	output, err := command.RunCommand(
		GetJavaLatest(),
		[]string{"-Djava.io.tmpdir=" + tmpDir, "-Xmx256m", "-cp", jar, "com.tencent.devops.agent.AgentVersionKt"},
		workDir,
		nil,
	)

	if err != nil {
		w.logs.Errorf("detect worker version failed: %s, output: %s", err.Error(), string(output))
		exitcode.CheckSignalWorkerError(err)
		w.SetVersion("")
		return ""
	}

	detectVersion := w.parseWorkerVersion(string(output))

	// 更新下 worker 的版本信息
	if detectVersion == "" {
		w.logs.Warn("parseWorkerVersion null")
	} else {
		w.SetVersion(detectVersion)
	}

	return detectVersion
}

// parseWorkerVersion 解析worker版本
func (w *WorkerType) parseWorkerVersion(output string) string {
	// 用函数匹配正确的版本信息, 主要解决tmp空间不足的情况下，jvm会打印出提示信息，导致识别不到worker版本号
	// 兼容旧版本，防止新agent发布后无限升级
	versionRegexp := regexp.MustCompile(`^v(\d+\.)(\d+\.)(\d+)((-RELEASE)|(-SNAPSHOT)?)$`)
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if !(line == "") && !strings.Contains(line, " ") && !strings.Contains(line, "OPTIONS") {
			if len(line) > 64 {
				line = line[:64]
			}
			// 先使用新版本的匹配逻辑匹配，匹配不通则使用旧版本
			if w.matchWorkerVersion(line) {
				w.logs.Info("match worker version: ", line)
				return line
			} else {
				if versionRegexp != nil {
					if versionRegexp.MatchString(line) {
						w.logs.Info("regexp worker version: ", line)
						return line
					} else {
						continue
					}
				} else {
					// 当正则式出错时(versionRegexp = nil)，继续使用原逻辑
					w.logs.Info("regexp nil worker version: ", line)
					return line
				}
			}
		}
	}
	return ""
}

// matchWorkerVersion 匹配worker版本信息
// 版本号为 v数字.数字.数字 || v数字.数字.数字-字符.数字
// 只匹配以v开头的数字版本即可
func (w *WorkerType) matchWorkerVersion(line string) bool {
	if !strings.HasPrefix(line, "v") {
		w.logs.Warnf("line %s matchWorkerVersion no start 'v'", line)
		return false
	}

	// 去掉v方便后面计算
	subline := strings.Split(strings.TrimPrefix(line, "v"), ".")
	sublen := len(subline)
	if sublen < 3 || sublen > 4 {
		w.logs.Warnf("line %s matchWorkerVersion len no match", line)
		return false
	}

	// v数字.数字.数字 这种去掉v后应该全是数字
	if sublen == 3 {
		return w.checkNumb(subline, line)
	}

	// v数字.数字.数字-字符.数字，按照 - 分隔，前面的与len 3一致，后面的两个分别判断，不是数字的是字符，不是字符的是数字
	fSubline := strings.Split(strings.TrimPrefix(line, "v"), "-")
	if len(fSubline) != 2 {
		w.logs.Warnf("line %s matchWorkerVersion len no match", line)
		return false
	}

	if !w.checkNumb(strings.Split(fSubline[0], "."), line) {
		return false
	}

	fSubline2 := strings.Split(fSubline[1], ".")
	if w.checkNumb([]string{fSubline2[0]}, line) {
		w.logs.Warnf("line %s matchWorkerVersion not char", line)
		return false
	}

	if !w.checkNumb([]string{fSubline2[1]}, line) {
		return false
	}

	return true
}

func (w *WorkerType) checkNumb(subs []string, line string) bool {
	for _, s := range subs {
		_, err := strconv.ParseInt(s, 10, 64)
		if err != nil {
			w.logs.Warnf("line %s matchWorkerVersion not numb", line)
			return false
		}
	}
	return true
}
