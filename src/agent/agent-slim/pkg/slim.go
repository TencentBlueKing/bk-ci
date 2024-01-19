package pkg

import (
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/cron"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/job"
)

func Run() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent run panic: ", err)
		}
	}()

	// 初始化国际化
	i18n.InitAgentI18n()

	go cron.CleanLog()

	job.DoPollAndBuild()
}
