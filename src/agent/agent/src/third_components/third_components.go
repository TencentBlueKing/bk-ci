package third_components

import (
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

// 用来处理一些Agent使用的第三方组件信息

// Init 初始化第三方组件
// 需要在日志初始化后
func Init() error {
	if err := initOb(); err != nil {
		return err
	}

	Worker.DetectWorkerVersion()

	return nil
}

func initOb() error {
	if logs.Logs == nil {
		return errors.New("init third components need init log")
	}

	Jdk = &JdkType{
		Jdk8: JdkVersionType{
			jdkModTime: time.Time{},
			version:    nil,
			lock:       sync.RWMutex{},
			logs: logs.Logs.WithFields(logrus.Fields{
				"third_component": "jdk8",
			}),
			vNum: jdk8,
		},
		Jdk17: JdkVersionType{
			jdkModTime: time.Time{},
			version:    nil,
			lock:       sync.RWMutex{},
			logs: logs.Logs.WithFields(logrus.Fields{
				"third_component": "jdk17",
			}),
			vNum: jdk17,
		},
	}

	Worker = &WorkerType{
		version: "",
		lock:    sync.RWMutex{},
		logs: logs.Logs.WithFields(logrus.Fields{
			"third_component": "worker",
		}),
	}

	return nil
}
