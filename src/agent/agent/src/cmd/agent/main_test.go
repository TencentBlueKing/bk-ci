package main

import (
	"reflect"
	"testing"
)

func TestResolveCLIArgs(t *testing.T) {
	tests := []struct {
		name    string
		args    []string
		want    []string
		wantCLI bool
	}{
		{
			name:    "no_args",
			args:    []string{"devopsAgent"},
			want:    nil,
			wantCLI: false,
		},
		{
			name:    "known_subcommand",
			args:    []string{"devopsAgent", "version"},
			want:    []string{"version"},
			wantCLI: true,
		},
		{
			name:    "unknown_subcommand_still_dispatches_cli",
			args:    []string{"devopsAgent", "foobar"},
			want:    []string{"foobar"},
			wantCLI: true,
		},
		{
			name:    "subcommand_with_flags",
			args:    []string{"devopsAgent", "version", "-f"},
			want:    []string{"version", "-f"},
			wantCLI: true,
		},
		{
			name:    "unknown_subcommand_with_extra_args",
			args:    []string{"devopsAgent", "foobar", "--x"},
			want:    []string{"foobar", "--x"},
			wantCLI: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotCLI := resolveCLIArgs(tt.args)
			if gotCLI != tt.wantCLI {
				t.Errorf("resolveCLIArgs(%v) cli = %v, want %v", tt.args, gotCLI, tt.wantCLI)
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("resolveCLIArgs(%v) args = %v, want %v", tt.args, got, tt.want)
			}
		})
	}
}
