package imagedebug

import "testing"

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
