package util

import (
	"encoding/json"
	"errors"
	"time"
)

// 需要给timeDuration 起一个别名方便json解码
// https://stackoverflow.com/questions/48050945/how-to-unmarshal-json-into-durations
type Duration time.Duration

func (dura *Duration) UnmarshalJSON(input []byte) error {
	var data interface{}
	if err := json.Unmarshal(input, &data); err != nil {
		return err
	}
	switch value := data.(type) {
	case float64:
		*dura = Duration(time.Duration(value))
		return nil
	case string:
		tmp, err := time.ParseDuration(value)
		if err != nil {
			return err
		}
		*dura = Duration(tmp)
		return nil
	default:
		return errors.New("invalid duration")
	}
}

func (d Duration) MarshalJSON() ([]byte, error) {
	return json.Marshal(time.Duration(d).String())
}

func (d Duration) String() string {
	return time.Duration(d).String()
}
