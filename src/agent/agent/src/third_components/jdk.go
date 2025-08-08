package third_components

import (
	"os"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	commonutil "github.com/TencentBlueKing/bk-ci/agentcommon/utils"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

var Jdk *JdkType

type jdkVersionNum int

const (
	jdk8  jdkVersionNum = 8
	jdk17 jdkVersionNum = 17
)

// JdkType 用来保存JDK版本信息
type JdkType struct {
	Jdk8  JdkVersionType
	Jdk17 JdkVersionType
}

// JdkVersionType jdk版本信息缓存
type JdkVersionType struct {
	// jdkModTime java应用修改时间
	jdkModTime time.Time
	version    []string
	lock       sync.RWMutex
	logs       *logrus.Entry
	vNum       jdkVersionNum
}

func (j *JdkVersionType) IsNull() bool {
	return commonutil.IsStringSliceBlank(j.GetVersion())
}

func (j *JdkVersionType) GetVersion() []string {
	j.lock.RLock()
	defer j.lock.RUnlock()
	return j.version
}

func (j *JdkVersionType) GetJdkModTime() time.Time {
	j.lock.RLock()
	defer j.lock.RUnlock()
	return j.jdkModTime
}

func (j *JdkVersionType) SetVersionAndTime(v []string, t time.Time) {
	j.lock.Lock()
	defer j.lock.Unlock()
	j.version = v
	j.jdkModTime = t
}

// SyncJdkVersion 同步jdk版本信息
func (j *JdkVersionType) SyncJdkVersion() error {
	jdkPath := config.GAgentConfig.Jdk17DirPath
	if j.vNum == jdk8 {
		jdkPath = config.GAgentConfig.JdkDirPath
	}

	// 获取jdk文件状态以及时间
	stat, err := os.Stat(jdkPath)
	if err != nil {
		if os.IsNotExist(err) {
			j.logs.Warnf("syncJdkVersion no %s find", jdkPath)
			// jdk版本置为空，否则会一直保持有版本的状态
			j.SetVersionAndTime([]string{}, time.Time{})
			return nil
		}
		return errors.Wrapf(err, "stat jdk %s error", jdkPath)
	}
	nowModTime := stat.ModTime()

	// 如果为空则必获取
	if j.IsNull() {
		logs.Debugf("is null get jdkversion %+v", j.GetVersion())
		version, err := j.getJdkVersion()
		if err != nil {
			// 拿取错误时直接下载新的
			j.logs.WithError(err).Error("jVersion is null getJdkVersion err")
			return nil
		}
		j.SetVersionAndTime(version, nowModTime)
		logs.Debugf("is null set jdkversion %+v", j.GetVersion())
		return nil
	}

	// 判断文件夹最后修改时间，不一致时不用更改
	if nowModTime == j.GetJdkModTime() {
		return nil
	}

	// 最后修改时间不一致需要重新获取版本信息
	version, err := j.getJdkVersion()
	if err != nil {
		// 拿取错误时直接下载新的
		j.logs.WithError(err).Error("exist jVersion getJdkVersion err")
		j.SetVersionAndTime(nil, time.Time{})
		return nil
	}
	j.SetVersionAndTime(version, nowModTime)
	return nil
}

func (j *JdkVersionType) getJdkVersion() ([]string, error) {
	var jdkVersion = make([]byte, 0)
	var err error
	switch j.vNum {
	case jdk8:
		jdkVersion, err = command.RunCommand(j.GetJava(), []string{"-version"}, "", nil)
	case jdk17:
		jdkVersion, err = command.RunCommand(j.GetJava(), []string{"--version"}, "", nil)
	}

	if err != nil {
		exitcode.CheckSignalJdkError(err)
		return nil, errors.Wrapf(err, "agent get jdk %d version failed", j.vNum)
	}
	var jdkV []string
	if jdkVersion == nil {
		return jdkV, nil
	}

	versionOutputString := strings.TrimSpace(string(jdkVersion))
	switch j.vNum {
	case jdk8:
		jdkV = trimJdk8VersionList(versionOutputString)
	case jdk17:
		jdkV = trimJdk17VersionList(versionOutputString)
	}
	j.logs.Infof("getJdkVersion %d %+v", j.vNum, string(jdkVersion))
	return jdkV, nil
}

// trimJdk8VersionList 清洗jdk8在解析一些版本信息的干扰信息,避免因tmp空间满等导致识别不准确造成重复不断的升级
func trimJdk8VersionList(versionOutputString string) []string {
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

// trimJdk17VersionList 清洗jdk17在解析一些版本信息的干扰信息,避免因tmp空间满等导致识别不准确造成重复不断的升级
func trimJdk17VersionList(versionOutputString string) []string {
	/*
		OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:
		   32490
		Try using the -Djava.io.tmpdir= option to select an alternate temp location.

		openjdk 17.0.11 2024-04-23 LTS
		OpenJDK Runtime Environment TencentKonaJDK (build 17.0.11+1-LTS)
		OpenJDK 64-Bit Server VM TencentKonaJDK (build 17.0.11+1-LTS, mixed mode, sharing)
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
			if strings.Contains(strings.ToLower(lines[i]), "openjdk 17") {
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

const (
	macosJdkBinPath = "/Contents/Home/bin/java"
	linuxJdkBinPath = "/bin/java"
	winJdkBinPath   = "/bin/java.exe"
)

// GetJava 获取本地java路径，区分版本
func (j *JdkVersionType) GetJava() string {
	switch j.vNum {
	case jdk8:
		if systemutil.IsMacos() {
			return config.GAgentConfig.JdkDirPath + macosJdkBinPath
		} else if systemutil.IsWindows() {
			return config.GAgentConfig.JdkDirPath + winJdkBinPath
		} else {
			return config.GAgentConfig.JdkDirPath + linuxJdkBinPath
		}
	default:
		if systemutil.IsMacos() {
			return config.GAgentConfig.Jdk17DirPath + macosJdkBinPath
		} else if systemutil.IsWindows() {
			return config.GAgentConfig.Jdk17DirPath + winJdkBinPath
		} else {
			return config.GAgentConfig.Jdk17DirPath + linuxJdkBinPath
		}
	}
}

func (j *JdkVersionType) GetJavaOrNull() string {
	path := j.GetJava()
	if _, err := os.Stat(path); err != nil && !os.IsExist(err) {
		logs.WithError(err).Warnf("jdk %d stat %s error", j.vNum, path)
		return ""
	}
	return path
}

// GetJavaLatest 获取本地java路径，默认使用最新的，没有时使用旧的
func GetJavaLatest() string {
	jdk17path := Jdk.Jdk17.GetJavaOrNull()
	if jdk17path == "" {
		return Jdk.Jdk8.GetJava()
	}
	return jdk17path
}

func (j *JdkVersionType) SetJavaDir(dir string) {
	switch j.vNum {
	case jdk8:
		if dir == config.GAgentConfig.JdkDirPath {
			return
		}
		config.GAgentConfig.JdkDirPath = dir
		err := config.GAgentConfig.SaveConfig()
		if err != nil {
			j.logs.WithError(err).Errorf("saveJdk %d Dir %s failed", j.vNum, dir)
			return
		}
	default:
		if dir == config.GAgentConfig.Jdk17DirPath {
			return
		}
		config.GAgentConfig.Jdk17DirPath = dir
		err := config.GAgentConfig.SaveConfig()
		if err != nil {
			j.logs.WithError(err).Errorf("saveJdk %d Dir %s failed", j.vNum, dir)
			return
		}
	}
}
