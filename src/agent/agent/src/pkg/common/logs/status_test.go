package logs

import (
	"testing"
	"time"
)

// TestStatusLogInterval 验证合法时长可覆盖默认间隔，
// 并确保缺省、非法格式及非正数统一回退到五分钟，避免配置错误造成日志刷屏。
func TestStatusLogInterval(t *testing.T) {
	for _, tt := range []struct {
		value string
		want  time.Duration
	}{
		{"", 5 * time.Minute},
		{"invalid", 5 * time.Minute},
		{"0s", 5 * time.Minute},
		{"-1m", 5 * time.Minute},
		{"30s", 30 * time.Second},
		{"10m", 10 * time.Minute},
	} {
		t.Run(tt.value, func(t *testing.T) {
			t.Setenv("DEVOPS_AGENT_STATUS_LOG_INTERVAL", tt.value)
			if got := StatusLogInterval(); got != tt.want {
				t.Fatalf("interval = %s, want %s", got, tt.want)
			}
		})
	}
}
