package main

import (
	"k8s.io/component-base/logs"
	"k8s.io/kubernetes/cmd/kube-scheduler/app"
	"kubernetes-manager-schedule-plugin/pkg/bkdevopsschedulerplugin"
	"os"
)

func main() {
	command := app.NewSchedulerCommand(
		app.WithPlugin(bkdevopsschedulerplugin.Name, bkdevopsschedulerplugin.New),
	)

	logs.InitLogs()
	defer logs.FlushLogs()

	if err := command.Execute(); err != nil {
		os.Exit(1)
	}
}
