package third_components

import (
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/pkg/errors"
	"os"
	"strings"
	"sync/atomic"
	"time"
)

var JdkVersion = &JdkVersionType{}

// JdkVersionType jdk版本信息缓存
type JdkVersionType struct {
	JdkFileModTime time.Time
	// 版本信息，原子级的 []string
	version atomic.Value
}

func (j *JdkVersionType) GetVersion() []string {
	data := j.version.Load()
	if data == nil {
		return make([]string, 0)
	} else {
		return data.([]string)
	}
}

func (j *JdkVersionType) SetVersion(version []string) {
	if version == nil {
		version = []string{}
	}
	j.version.Swap(version)
}

// SyncJdkVersion 同步jdk版本信息
func SyncJdkVersion() error {
	// 获取jdk文件状态以及时间
	stat, err := os.Stat(config.GAgentConfig.JdkDirPath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.WithError(err).Error("syncJdkVersion no jdk dir find")
			// jdk版本置为空，否则会一直保持有版本的状态
			JdkVersion.SetVersion([]string{})
			return nil
		}
		return errors.Wrap(err, "agent check jdk dir error")
	}
	nowModTime := stat.ModTime()

	// 如果为空则必获取
	if len(JdkVersion.GetVersion()) == 0 {
		version, err := getJdkVersion()
		if err != nil {
			// 拿取错误时直接下载新的
			logs.WithError(err).Error("syncJdkVersion getJdkVersion err")
			return nil
		}
		JdkVersion.SetVersion(version)
		JdkVersion.JdkFileModTime = nowModTime
		return nil
	}

	// 判断文件夹最后修改时间，不一致时不用更改
	if nowModTime == JdkVersion.JdkFileModTime {
		return nil
	}

	version, err := getJdkVersion()
	if err != nil {
		// 拿取错误时直接下载新的
		logs.WithError(err).Error("syncJdkVersion getJdkVersion err")
		JdkVersion.SetVersion([]string{})
		return nil
	}
	JdkVersion.SetVersion(version)
	JdkVersion.JdkFileModTime = nowModTime
	return nil
}

func getJdkVersion() ([]string, error) {
	jdkVersion, err := command.RunCommand(config.GetJava(), []string{"-version"}, "", nil)
	if err != nil {
		logs.WithError(err).Error("agent get jdk version failed")
		exitcode.CheckSignalJdkError(err)
		return nil, errors.Wrap(err, "agent get jdk version failed")
	}
	var jdkV []string
	if jdkVersion != nil {
		versionOutputString := strings.TrimSpace(string(jdkVersion))
		jdkV = trimJdkVersionList(versionOutputString)
	}

	return jdkV, nil
}

// parseJdkVersionList 清洗在解析一些版本信息的干扰信息,避免因tmp空间满等导致识别不准确造成重复不断的升级
func trimJdkVersionList(versionOutputString string) []string {
	/*
		OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:
		   32490
		Try using the -Djava.io.tmpdir= option to select an alternate temp location.

		openjdk version "1.8.0_352"
		OpenJDK Runtime Environment (Tencent Kona 8.0.12) (build 1.8.0_352-b1)
		OpenJDK 64-Bit Server VM (Tencent Kona 8.0.12) (build 25.352-b1, mixed mode)
		Picked up _JAVA_OPTIONS: -Xmx8192m -Xms256m -Xss8m
	*/
	// 一个JVM版本只需要识别3行。
	var jdkV = make([]string, 3)

	var sep = "\n"
	if strings.HasSuffix(versionOutputString, "\r\n") {
		sep = "\r\n"
	}

	lines := strings.Split(strings.TrimSuffix(versionOutputString, sep), sep)

	var pos = 0
	for i := range lines {

		if pos == 0 {
			if strings.Contains(lines[i], " version ") {
				jdkV[pos] = lines[i]
				pos++
			}
		} else if pos == 1 {
			if strings.Contains(lines[i], " Runtime Environment ") {
				jdkV[pos] = lines[i]
				pos++
			}
		} else if pos == 2 {
			if strings.Contains(lines[i], " Server VM ") {
				jdkV[pos] = lines[i]
				break
			}
		}
	}

	return jdkV
}

func 