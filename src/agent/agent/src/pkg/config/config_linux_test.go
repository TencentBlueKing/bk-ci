//go:build linux

package config

import (
	"strings"
	"testing"
)

func TestParseOSRelease(t *testing.T) {
	tests := []struct {
		name    string
		input   string
		wantID  string
		wantVer string
		wantErr bool
	}{
		{
			name:    "ubuntu",
			input:   "NAME=\"Ubuntu\"\nVERSION_ID=\"22.04\"\nID=ubuntu\nPRETTY_NAME=\"Ubuntu 22.04.3 LTS\"\n",
			wantID:  "ubuntu",
			wantVer: "22.04",
		},
		{
			name:    "centos",
			input:   "NAME=\"CentOS Linux\"\nVERSION_ID=\"7\"\nID=\"centos\"\n",
			wantID:  "centos",
			wantVer: "7",
		},
		{
			name:    "empty_input",
			input:   "",
			wantID:  "",
			wantVer: "",
		},
		{
			name:    "missing_id",
			input:   "NAME=\"SomeOS\"\nVERSION_ID=\"1.0\"\n",
			wantID:  "",
			wantVer: "1.0",
		},
		{
			name:    "malformed_lines_ignored",
			input:   "NOEQUALS\nID=alpine\nBAD\nVERSION_ID=3.18\n",
			wantID:  "alpine",
			wantVer: "3.18",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			info, err := parseOSRelease(strings.NewReader(tt.input))
			if (err != nil) != tt.wantErr {
				t.Fatalf("parseOSRelease() error = %v, wantErr %v", err, tt.wantErr)
			}
			if err != nil {
				return
			}
			if info.ID != tt.wantID {
				t.Errorf("ID = %q, want %q", info.ID, tt.wantID)
			}
			if info.VersionID != tt.wantVer {
				t.Errorf("VersionID = %q, want %q", info.VersionID, tt.wantVer)
			}
		})
	}
}

func TestCharsToString(t *testing.T) {
	tests := []struct {
		name  string
		input []byte
		want  string
	}{
		{"normal", []byte("hello"), "hello"},
		{"with_null", []byte{'h', 'i', 0, 'x', 'y'}, "hi"},
		{"all_null", []byte{0, 0, 0}, ""},
		{"no_null", []byte{65, 66, 67}, "ABC"},
		{"empty", []byte{}, ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := charsToString(tt.input); got != tt.want {
				t.Errorf("charsToString(%v) = %q, want %q", tt.input, got, tt.want)
			}
		})
	}
}
