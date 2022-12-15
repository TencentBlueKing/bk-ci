package config

import (
	"github.com/Tencent/bk-ci/src/agent/src/pkg/logs"
	"os"
	"reflect"
	"testing"
)

func Test_parseWorkerVersion(t *testing.T) {
	logFile := "config_unit_test.log"
	_ = logs.Init(logFile)

	defer func() { _ = os.Remove(logFile) }()

	tests := []struct {
		name  string
		lines string
		want  string
	}{
		{
			name: "Insufficient memory",
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
