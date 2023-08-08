package config

import (
	"os"
	"reflect"
	"testing"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
)

func Test_parseWorkerVersion(t *testing.T) {
	logFile := "config_unit_test.log"
	_ = logs.Init(logFile, false)

	defer func() { _ = os.Remove(logFile) }()

	tests := []struct {
		name  string
		lines string
		want  string
	}{
		// 兼容旧版本，防止新agent发布后无限升级
		{
			name: "Insufficient memory old",
			lines: "OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:\n" +
				"   30458\n" +
				"Try using the -Djava.io.tmpdir= option to select an alternate temp location.\n\n" +
				"v1.13.8-RELEASE",
			want: "v1.13.8-RELEASE",
		},
		{
			name:  "Normal: with RELEASE",
			lines: "v1.13.8-RELEASE\n",
			want:  "v1.13.8-RELEASE",
		},
		{
			name:  "Normal: without RELEASE",
			lines: "v1.13.8\r\n",
			want:  "v1.13.8",
		},
		{
			name:  "Normal: with SNAPSHOT",
			lines: "v1.13.8-SNAPSHOT\r\n",
			want:  "v1.13.8-SNAPSHOT",
		},
		{
			name: "Insufficient memory",
			lines: "OpenJDK 64-Bit Server VM warning: Insufficient space for shared memory file:\n" +
				"   30458\n" +
				"Try using the -Djava.io.tmpdir= option to select an alternate temp location.\n\n" +
				"v1.13.8",
			want: "v1.13.8",
		},
		{
			name:  "Normal: with suffix",
			lines: "v1.13.8-beta.4",
			want:  "v1.13.8-beta.4",
		},
		{
			name:  "Normal: with suffix",
			lines: "v1.13.8-rc.241",
			want:  "v1.13.8-rc.241",
		},
		{
			name:  "Normal: with suffix",
			lines: "v1.13.8-alpha.22",
			want:  "v1.13.8-alpha.22",
		},
		{
			name:  "Normal: without suffix",
			lines: "v1.13.8\r\n",
			want:  "v1.13.8",
		},
		{
			name:  "illegal: only number",
			lines: "12356\n",
			want:  "",
		},
		{
			name:  "illegal: bad format",
			lines: "v3.1.1.1",
			want:  "",
		},
		{
			name:  "illegal: end with -",
			lines: "v1.13.8-\r\n",
			want:  "",
		},
	}

	for _, tt := range tests {

		t.Run(tt.name, func(t *testing.T) {
			version := parseWorkerVersion(tt.lines)
			if !reflect.DeepEqual(version, tt.want) {
				t.Fatalf("Fail: %v = %v, want %v", tt.name, version, tt.want)
			}
		})
	}

}

func Test_matchWorkerVersion(t *testing.T) {
	type args struct {
		line string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "normal",
			args: args{
				line: "v1.10.0-beta.55",
			},
			want: true,
		},
	}
	logs.UNTestDebugInit()
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := matchWorkerVersion(tt.args.line); got != tt.want {
				t.Errorf("matchWorkerVersion() = %v, want %v", got, tt.want)
			}
		})
	}
}
