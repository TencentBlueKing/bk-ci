package proxy

import "testing"

const (
	testSuffix = ".debug.com"
)

func Test_parseWsHostName(t *testing.T) {
	type args struct {
		wsHostSuffix string
		hostname     string
		matchPort    bool
	}
	tests := []struct {
		name              string
		args              args
		wantWorkspaceID   string
		wantWorkspacePort string
		wantOk            bool
	}{
		{
			name: "输入为空",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "",
				matchPort:    false,
			},
			wantWorkspaceID:   "",
			wantWorkspacePort: "",
			wantOk:            false,
		},
		{
			name: "输入错误",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "x-x-x-xaaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "",
			wantWorkspacePort: "",
			wantOk:            false,
		},
		{
			name: "输入错误",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "aaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "",
			wantWorkspacePort: "",
			wantOk:            false,
		},
		{
			name: "输入错误",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "x-x-x-x-aaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "",
			wantWorkspacePort: "",
			wantOk:            false,
		},
		{
			name: "测试情况1、userid-string.hostsuffix",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "test-aaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "test-aaa",
			wantWorkspacePort: "",
			wantOk:            true,
		},
		{
			name: "测试情况2、port-userid-string.hostsuffix",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "8080-test-aaa" + testSuffix,
				matchPort:    true,
			},
			wantWorkspaceID:   "test-aaa",
			wantWorkspacePort: "8080",
			wantOk:            true,
		},
		{
			name: "测试情况2、port-userid-string.hostsuffix 错误",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "8080-test-aaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "",
			wantWorkspacePort: "",
			wantOk:            false,
		},
		{
			name: "测试情况3、userid(v-xxx)-string.hostsuffix",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "v-test-aaa" + testSuffix,
				matchPort:    false,
			},
			wantWorkspaceID:   "v-test-aaa",
			wantWorkspacePort: "",
			wantOk:            true,
		},
		{
			name: "测试情况4、port-userid(v-xxx)-string.hostsuffix",
			args: args{
				wsHostSuffix: testSuffix,
				hostname:     "3000-v-test-aaa" + testSuffix,
				matchPort:    true,
			},
			wantWorkspaceID:   "v-test-aaa",
			wantWorkspacePort: "3000",
			wantOk:            true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotWorkspaceID, gotWorkspacePort, gotOk := parseWsHostName(tt.args.wsHostSuffix, tt.args.hostname, tt.args.matchPort)
			if gotWorkspaceID != tt.wantWorkspaceID {
				t.Errorf("parseWsHostName() gotWorkspaceID = %v, want %v", gotWorkspaceID, tt.wantWorkspaceID)
			}
			if gotWorkspacePort != tt.wantWorkspacePort {
				t.Errorf("parseWsHostName() gotWorkspacePort = %v, want %v", gotWorkspacePort, tt.wantWorkspacePort)
			}
			if gotOk != tt.wantOk {
				t.Errorf("parseWsHostName() gotOk = %v, want %v", gotOk, tt.wantOk)
			}
		})
	}
}
