package item

import (
	"os"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/pkg/errors"
)

// DockerFileMd5 缓存，用来计算md5
var TelegrafConf *TelegrafConfType

func init() {
	DockerFileMd5 = &DockerFileMd5Type{
		needUpgrade: false,
	}
}

type TelegrafConfType struct {
	fileModTime time.Time
	lock        sync.RWMutex
	md5         string
}

func (t *TelegrafConfType) Read() string {
	t.lock.RLock()
	defer t.lock.RUnlock()
	return t.md5
}

func (t *TelegrafConfType) Sync() error {
	t.lock.Lock()
	defer func() {
		t.lock.Unlock()
	}()

	filePath := config.GetTelegrafConfFilePath()

	stat, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			logs.Warn("TelegrafConf sync no telegraf conf file find", err)
			t.md5 = ""
			return nil
		}
		return errors.Wrap(err, "agent check telegraf conf file error")
	}
	nowModTime := stat.ModTime()

	if t.md5 == "" {
		t.md5, err = fileutil.GetFileMd5(filePath)
		if err != nil {
			t.md5 = ""
			return errors.Wrapf(err, "agent get telegraf conf file %s md5 error", filePath)
		}
		t.fileModTime = nowModTime
		return nil
	}

	if nowModTime == t.fileModTime {
		return nil
	}

	t.md5, err = fileutil.GetFileMd5(filePath)
	if err != nil {
		t.md5 = ""
		return errors.Wrapf(err, "agent get telegraf conf file %s md5 error", filePath)
	}
	t.fileModTime = nowModTime
	return nil
}
