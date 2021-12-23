/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

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
