package rsa

import (
	"testing"
)

func TestRsaAll(t *testing.T) {
	tests := []struct {
		name       string
		input      string
		wantOutput string
		wantErr    bool
	}{
		{
			name:       "rsa全量测试",
			input:      "我爱吃肉",
			wantOutput: "我爱吃肉",
			wantErr:    false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotPrvkey, gotPubkey, err := GenRsaKey()
			if (err != nil) != tt.wantErr {
				t.Errorf("GenRsaKey() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			encoded, err := RSAEncrypt([]byte(tt.input), gotPubkey)
			if (err != nil) != tt.wantErr {
				t.Errorf("RSAEncrypt() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			decoded, err := RSADecrypt(encoded, gotPrvkey)
			if (err != nil) != tt.wantErr {
				t.Errorf("RSADecrypt() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if decoded != tt.wantOutput {
				t.Errorf("result not equal get %s want %s", decoded, tt.wantOutput)
			}
		})
	}
}
