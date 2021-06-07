package compress

import (
	"bytes"
	"compress/gzip"
	"encoding/base64"
)

func ToBase64String(src []byte) string {
	var b bytes.Buffer
	gz := gzip.NewWriter(&b)
	_, _ = gz.Write(src)
	_ = gz.Close()

	return base64.StdEncoding.EncodeToString(b.Bytes())
}
