package job

import (
	"testing"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

func Test_getUnixWorkerPrepareStartScriptFile(t *testing.T) {
	type args struct {
		projectId string
		buildId   string
		vmSeqId   string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			"测试Unix prepare start文件生成",
			args{"1", "2", "3"},
			systemutil.GetWorkDir() + "/devops_agent_prepare_start_1_2_3.sh",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getUnixWorkerPrepareStartScriptFile(tt.args.projectId, tt.args.buildId, tt.args.vmSeqId); got != tt.want {
				t.Errorf("getUnixWorkerPrepareStartScriptFile() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getUnixWorkerStartScriptFile(t *testing.T) {
	type args struct {
		projectId string
		buildId   string
		vmSeqId   string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			"测试Unix start文件生成",
			args{"1", "2", "3"},
			systemutil.GetWorkDir() + "/devops_agent_start_1_2_3.sh",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getUnixWorkerStartScriptFile(tt.args.projectId, tt.args.buildId, tt.args.vmSeqId); got != tt.want {
				t.Errorf("getUnixWorkerStartScriptFile() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getWorkerErrorMsgFile(t *testing.T) {
	type args struct {
		buildId string
		vmSeqId string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			"测试Worker error msg文件生成",
			args{"1", "2"},
			systemutil.GetWorkDir() + "/build_tmp/1_2_build_msg.log",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getWorkerErrorMsgFile(tt.args.buildId, tt.args.vmSeqId); got != tt.want {
				t.Errorf("getWorkerErrorMsgFile() = %v, want %v", got, tt.want)
			}
		})
	}
}
