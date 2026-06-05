//go:build windows
// +build windows

package monitor

import "testing"

func TestNormalizeDiskPathTagWindows(t *testing.T) {
	tests := []struct {
		name string
		path string
		want string
	}{
		{name: "drive letter", path: "C:", want: "\\C:"},
		{name: "lowercase drive letter", path: "c:", want: "\\C:"},
		{name: "drive root slash", path: "C:\\", want: "\\C:"},
		{name: "drive root forward slash", path: "D:/", want: "\\D:"},
		{name: "already normalized", path: "\\E:", want: "\\E:"},
		{name: "already normalized lowercase", path: "\\f:", want: "\\F:"},
		{name: "non drive path", path: "/", want: "/"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := normalizeDiskPathTag(tt.path); got != tt.want {
				t.Fatalf("normalizeDiskPathTag(%q)=%q, want %q", tt.path, got, tt.want)
			}
		})
	}
}
