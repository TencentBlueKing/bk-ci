package utils

import "testing"

func TestIsStringSliceBlank(t *testing.T) {
	tests := []struct {
		name  string
		slice []string
		want  bool
	}{
		{"nil_slice", nil, true},
		{"empty_slice", []string{}, true},
		{"single_empty", []string{""}, true},
		{"all_empty", []string{"", "", ""}, true},
		{"single_nonempty", []string{"a"}, false},
		{"mixed_with_nonempty", []string{"", "a", ""}, false},
		{"all_nonempty", []string{"a", "b", "c"}, false},
		{"whitespace_is_nonempty", []string{" "}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := IsStringSliceBlank(tt.slice)
			if got != tt.want {
				t.Errorf("IsStringSliceBlank(%v) = %v, want %v", tt.slice, got, tt.want)
			}
		})
	}
}
