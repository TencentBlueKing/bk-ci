//go:build !out
// +build !out

package monitor

import (
	"reflect"
	"testing"
)

// rename.go（非 out / 内部版）退化为直通。测试用例仅验证：
//   1. nil / 空输入安全
//   2. 对规范名 metric（input 层直接产出的形态）做 Rename 不改任何内容
//   3. Rename 不修改入参 slice 的底层数据

func TestRename_Empty(t *testing.T) {
	if got := Rename(nil); got != nil {
		t.Errorf("Rename(nil) = %v, want nil", got)
	}
	if got := Rename([]Metric{}); len(got) != 0 {
		t.Errorf("Rename(empty) = %v, want empty", got)
	}
}

func TestRename_PassThrough_CPUDetail(t *testing.T) {
	in := []Metric{{
		Name: RenamedCPUDetail,
		Fields: map[string]interface{}{
			RenamedFieldUser:   1.0,
			RenamedFieldSystem: 2.0,
			RenamedFieldIdle:   97.0,
			RenamedFieldIowait: 0.0,
		},
	}}
	got := Rename(in)[0]
	if got.Name != RenamedCPUDetail {
		t.Errorf("measurement should remain %q, got %q", RenamedCPUDetail, got.Name)
	}
	wantFields := map[string]interface{}{
		RenamedFieldUser:   1.0,
		RenamedFieldSystem: 2.0,
		RenamedFieldIdle:   97.0,
		RenamedFieldIowait: 0.0,
	}
	if !reflect.DeepEqual(got.Fields, wantFields) {
		t.Errorf("cpu_detail fields changed:\n got  %v\n want %v", got.Fields, wantFields)
	}
}

func TestRename_PassThrough_Disk(t *testing.T) {
	in := []Metric{{
		Name: MeasurementDisk,
		Fields: map[string]interface{}{
			RenamedFieldInUse: 80.0,
			FieldTotal:        100.0,
		},
	}}
	got := Rename(in)[0]
	if got.Name != MeasurementDisk {
		t.Errorf("disk measurement should remain, got %q", got.Name)
	}
	if v, ok := got.Fields[RenamedFieldInUse]; !ok || v != 80.0 {
		t.Errorf("in_use should stay, got %v", got.Fields)
	}
}

func TestRename_PassThrough_IO(t *testing.T) {
	in := []Metric{{
		Name: RenamedIO,
		Fields: map[string]interface{}{
			RenamedFieldRkbS: uint64(100),
			RenamedFieldWkbS: uint64(200),
			FieldReads:       uint64(10),
		},
	}}
	got := Rename(in)[0]
	if got.Name != RenamedIO {
		t.Errorf("io measurement should remain, got %q", got.Name)
	}
	for _, k := range []string{RenamedFieldRkbS, RenamedFieldWkbS, FieldReads} {
		if _, ok := got.Fields[k]; !ok {
			t.Errorf("field %q should remain", k)
		}
	}
}

func TestRename_PassThrough_Env(t *testing.T) {
	in := []Metric{{
		Name: RenamedEnv,
		Fields: map[string]interface{}{
			FieldUptime:       uint64(1700000000),
			RenamedFieldProcs: uint64(99),
		},
	}}
	got := Rename(in)[0]
	if got.Name != RenamedEnv {
		t.Errorf("env measurement should remain, got %q", got.Name)
	}
	for _, k := range []string{FieldUptime, RenamedFieldProcs} {
		if _, ok := got.Fields[k]; !ok {
			t.Errorf("field %q should remain", k)
		}
	}
}

func TestRename_PassThrough_Load(t *testing.T) {
	in := []Metric{{
		Name:   RenamedLoad,
		Fields: map[string]interface{}{FieldLoad1: 0.5},
	}}
	got := Rename(in)[0]
	if got.Name != RenamedLoad {
		t.Errorf("load measurement should remain, got %q", got.Name)
	}
}

func TestRename_PassThrough_Netstat(t *testing.T) {
	in := []Metric{{
		Name: MeasurementNetstat,
		Fields: map[string]interface{}{
			RenamedFieldCurTCPEstab:    int64(100),
			RenamedFieldCurTCPTimeWait: int64(50),
		},
	}}
	got := Rename(in)[0]
	if got.Name != MeasurementNetstat {
		t.Errorf("netstat measurement should remain, got %q", got.Name)
	}
	for _, k := range []string{RenamedFieldCurTCPEstab, RenamedFieldCurTCPTimeWait} {
		if _, ok := got.Fields[k]; !ok {
			t.Errorf("field %q should remain", k)
		}
	}
}

func TestRename_DoesNotMutateInput(t *testing.T) {
	in := []Metric{{
		Name:   RenamedCPUDetail,
		Fields: map[string]interface{}{RenamedFieldUser: 1.0},
	}}
	origName := in[0].Name
	origFields := map[string]interface{}{}
	for k, v := range in[0].Fields {
		origFields[k] = v
	}
	Rename(in)
	if in[0].Name != origName {
		t.Error("input measurement must not be mutated")
	}
	if !reflect.DeepEqual(in[0].Fields, origFields) {
		t.Errorf("input fields mutated: %v vs %v", in[0].Fields, origFields)
	}
}

func TestRenameWindowsFields_PassThrough(t *testing.T) {
	in := []Metric{{
		Name: RenamedCPUDetail,
		Fields: map[string]interface{}{
			RenamedFieldUser: 5.0,
			RenamedFieldIdle: 93.0,
		},
	}}
	got := RenameWindowsFields(in)[0]
	if got.Name != RenamedCPUDetail {
		t.Errorf("measurement should remain, got %q", got.Name)
	}
	for _, want := range []string{RenamedFieldUser, RenamedFieldIdle} {
		if _, ok := got.Fields[want]; !ok {
			t.Errorf("field %q should remain", want)
		}
	}
}
