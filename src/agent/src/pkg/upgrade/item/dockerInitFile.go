package item

import (
	"os"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/pkg/errors"
)

// DockerFileMd5 缓存，用来计算md5
var DockerFileMd5 *DockerFileMd5Type

func init() {
	DockerFileMd5 = &DockerFileMd5Type{
		needUpgrade: false,
	}
}

type DockerFileMd5Type struct {
	// 目前非linux机器不支持，以及一些机器不使用docker就不用计算md5
	needUpgrade bool
	fileModTime time.Time
	lock        sync.RWMutex
	md5         string
}

func (d *DockerFileMd5Type) Read() (string, bool) {
	d.lock.RLock()
	defer d.lock.RUnlock()
	return d.md5, d.needUpgrade
}

func (d *DockerFileMd5Type) Sync() error {
	if !systemutil.IsLinux() || !config.GAgentConfig.EnableDockerBuild {
		d.needUpgrade = false
		return nil
	}
	d.lock.Lock()
	defer func() {
		d.lock.Unlock()
	}()
	d.needUpgrade = true

	filePath := config.GetDockerInitFilePath()

	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.Warn("syncDockerInitFileMd5 no docker init file find", err)
			d.md5 = ""
			return nil
		}
		return errors.Wrap(err, "agent check docker init file error")
	}
	nowModTime := stat.ModTime()

	if d.md5 == "" {
		d.md5, err = fileutil.GetFileMd5(filePath)
		if err != nil {
			d.md5 = ""
			return errors.Wrapf(err, "agent get docker init file %s md5 error", filePath)
		}
		d.fileModTime = nowModTime
		return nil
	}

	if nowModTime == d.fileModTime {
		return nil
	}

	d.md5, err = fileutil.GetFileMd5(filePath)
	if err != nil {
		d.md5 = ""
		return errors.Wrapf(err, "agent get docker init file %s md5 error", filePath)
	}
	d.fileModTime = nowModTime
	return nil
}
