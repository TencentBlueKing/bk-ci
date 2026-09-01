package httputil

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

func init() {
	logs.UNTestDebugInit()
}

func TestDevopsResult_IsOk(t *testing.T) {
	tests := []struct {
		name   string
		status int64
		wantOk bool
	}{
		{"status_0", 0, true},
		{"status_1", 1, false},
		{"status_neg1", -1, false},
		{"status_200", 200, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := &DevopsResult{Status: tt.status}
			if d.IsOk() != tt.wantOk {
				t.Errorf("IsOk() = %v, want %v", d.IsOk(), tt.wantOk)
			}
			if d.IsNotOk() == tt.wantOk {
				t.Errorf("IsNotOk() = %v, want %v", d.IsNotOk(), !tt.wantOk)
			}
		})
	}
}

func TestAgentResult_IsAgentDelete(t *testing.T) {
	tests := []struct {
		name        string
		agentStatus string
		want        bool
	}{
		{"delete", config.AgentStatusDelete, true},
		{"empty", "", false},
		{"other", "IMPORT_OK", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &AgentResult{AgentStatus: tt.agentStatus}
			if got := a.IsAgentDelete(); got != tt.want {
				t.Errorf("IsAgentDelete() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHttpResult_IntoDevopsResult(t *testing.T) {
	t.Run("valid_json", func(t *testing.T) {
		body, _ := json.Marshal(map[string]interface{}{
			"status":  0,
			"message": "success",
			"data":    map[string]string{"key": "value"},
		})
		r := &HttpResult{Body: body, Status: 200}
		result, err := r.IntoDevopsResult()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !result.IsOk() {
			t.Errorf("expected IsOk()=true, status=%d", result.Status)
		}
		if result.Message != "success" {
			t.Errorf("Message = %q, want %q", result.Message, "success")
		}
	})

	t.Run("with_error", func(t *testing.T) {
		r := &HttpResult{Error: fmt.Errorf("network error")}
		_, err := r.IntoDevopsResult()
		if err == nil {
			t.Error("expected error when HttpResult.Error is set")
		}
	})

	t.Run("invalid_json", func(t *testing.T) {
		r := &HttpResult{Body: []byte("not json"), Status: 200}
		_, err := r.IntoDevopsResult()
		if err == nil {
			t.Error("expected error for invalid JSON")
		}
	})
}

func TestHttpResult_IntoAgentResult(t *testing.T) {
	t.Run("valid_with_agent_status", func(t *testing.T) {
		body, _ := json.Marshal(map[string]interface{}{
			"status":      0,
			"agentStatus": "IMPORT_OK",
			"data":        nil,
		})
		r := &HttpResult{Body: body, Status: 200}
		result, err := r.IntoAgentResult()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if result.AgentStatus != "IMPORT_OK" {
			t.Errorf("AgentStatus = %q, want IMPORT_OK", result.AgentStatus)
		}
		if result.IsAgentDelete() {
			t.Error("should not be deleted")
		}
	})

	t.Run("delete_status", func(t *testing.T) {
		body, _ := json.Marshal(map[string]interface{}{
			"status":      0,
			"agentStatus": config.AgentStatusDelete,
		})
		r := &HttpResult{Body: body, Status: 200}
		result, err := r.IntoAgentResult()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !result.IsAgentDelete() {
			t.Error("expected IsAgentDelete()=true")
		}
	})

	t.Run("with_error", func(t *testing.T) {
		r := &HttpResult{Error: fmt.Errorf("timeout")}
		_, err := r.IntoAgentResult()
		if err == nil {
			t.Error("expected error")
		}
	})
}
