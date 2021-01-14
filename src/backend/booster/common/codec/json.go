package codec

import (
	"io"
	"reflect"

	"github.com/ugorji/go/codec"
)

var defaultJsonHandle = codec.JsonHandle{MapKeyAsString: true}

// DecJSON decode byte json into struct or map.
func DecJSON(s []byte, v interface{}) error {
	dec := codec.NewDecoderBytes(s, &defaultJsonHandle)
	return dec.Decode(v)
}

// DecJSONReader decode Reader json into struct or map.
func DecJSONReader(s io.Reader, v interface{}) error {
	dec := codec.NewDecoder(s, &defaultJsonHandle)
	return dec.Decode(v)
}

// EncJSON encode struct or map data into byte json.
func EncJSON(v interface{}, s *[]byte) error {
	enc := codec.NewEncoderBytes(s, &defaultJsonHandle)
	return enc.Encode(v)
}

// EncJSONWriter encode Writer data into byte json.
func EncJSONWriter(v interface{}, s io.Writer) error {
	enc := codec.NewEncoder(s, &defaultJsonHandle)
	return enc.Encode(v)
}

func init() {
	defaultJsonHandle.MapType = reflect.TypeOf(map[string]interface{}(nil))
}
