package imagedebug

import (
	"fmt"
	"net"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func Test_checkPortRangeFormat(t *testing.T) {
	type args struct {
		rg string
	}
	tests := []struct {
		name  string
		args  args
		want  int
		want1 int
		want2 bool
	}{
		{
			name: "not numb",
			args: args{
				rg: "a-b",
			},
			want:  0,
			want1: 0,
			want2: false,
		},
		{
			name: "not format",
			args: args{
				rg: "1=2",
			},
			want:  0,
			want1: 0,
			want2: false,
		},
		{
			name: "overflow",
			args: args{
				rg: "1333-1222",
			},
			want:  0,
			want1: 0,
			want2: false,
		},
		{
			name: "ok",
			args: args{
				rg: "30000-32767",
			},
			want:  30000,
			want1: 32767,
			want2: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1, got2 := checkPortRangeFormat(tt.args.rg)
			if got != tt.want {
				t.Errorf("checkPortRangeFormat() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("checkPortRangeFormat() got1 = %v, want %v", got1, tt.want1)
			}
			if got2 != tt.want2 {
				t.Errorf("checkPortRangeFormat() got2 = %v, want %v", got2, tt.want2)
			}
		})
	}
}

func TestAllocateNodePort(t *testing.T) {
	logs.UNTestDebugInit()
	Init()

	t.Run("returns_valid_port_in_range", func(t *testing.T) {
		pa := &PortAllocator{
			maxNodePortAllocRetries: 100,
			nodePortRangeSize:       100,
			nodePortRangeStart:      40000,
		}
		port, err := pa.AllocateNodePort()
		if err != nil {
			t.Fatalf("AllocateNodePort() error = %v", err)
		}
		if port < 40000 || port >= 40100 {
			t.Errorf("port %d out of range [40000, 40100)", port)
		}
	})

	t.Run("skips_occupied_port", func(t *testing.T) {
		ln, err := net.Listen("tcp", ":40050")
		if err != nil {
			t.Skipf("cannot bind test port: %v", err)
		}
		defer ln.Close()

		pa := &PortAllocator{
			maxNodePortAllocRetries: 200,
			nodePortRangeSize:       1,
			nodePortRangeStart:      40050,
		}
		_, err = pa.AllocateNodePort()
		if err == nil {
			t.Error("expected error when only port is occupied, got nil")
		}
	})

	t.Run("no_double_check_race", func(t *testing.T) {
		pa := &PortAllocator{
			maxNodePortAllocRetries: 50,
			nodePortRangeSize:       50,
			nodePortRangeStart:      41000,
		}
		for i := 0; i < 20; i++ {
			port, err := pa.AllocateNodePort()
			if err != nil {
				t.Fatalf("iteration %d: AllocateNodePort() error = %v", i, err)
			}
			ln, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
			if err != nil {
				t.Fatalf("iteration %d: port %d returned as free but cannot bind: %v", i, port, err)
			}
			ln.Close()
		}
	})
}
