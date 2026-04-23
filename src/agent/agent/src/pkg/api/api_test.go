package api

import "testing"

func TestLogEndpoint(t *testing.T) {
	tests := []struct {
		name  string
		color string
		want  string
	}{
		{"default_empty", "", "/ms/log/api/build/logs"},
		{"red", "red", "/ms/log/api/build/logs/red"},
		{"yellow", "yellow", "/ms/log/api/build/logs/yellow"},
		{"unknown_falls_back_normal", "blue", "/ms/log/api/build/logs"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := logEndpoint(tt.color); got != tt.want {
				t.Errorf("logEndpoint(%q) = %q, want %q", tt.color, got, tt.want)
			}
		})
	}
}
