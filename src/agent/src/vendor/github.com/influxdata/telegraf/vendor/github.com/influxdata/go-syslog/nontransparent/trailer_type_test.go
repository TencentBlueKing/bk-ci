package nontransparent

import (
	"encoding/json"

	"github.com/stretchr/testify/assert"

	"testing"
)

type trailerWrapper struct {
	Trailer TrailerType `json:"trailer"`
}

func TestUnmarshalTOML(t *testing.T) {
	var t1 TrailerType
	t1.UnmarshalTOML([]byte(`"LF"`))
	assert.Equal(t, LF, t1)

	var t2 TrailerType
	t2.UnmarshalTOML([]byte(`LF`))
	assert.Equal(t, LF, t2)

	var t3 TrailerType
	t3.UnmarshalTOML([]byte(`'LF'`))
	assert.Equal(t, LF, t3)

	var t4 TrailerType
	t4.UnmarshalTOML([]byte(`"NUL"`))
	assert.Equal(t, NUL, t4)

	var t5 TrailerType
	t5.UnmarshalTOML([]byte(`NUL`))
	assert.Equal(t, NUL, t5)

	var t6 TrailerType
	t6.UnmarshalTOML([]byte(`'NUL'`))
	assert.Equal(t, NUL, t6)

	var t7 TrailerType
	err := t7.UnmarshalTOML([]byte(`wrong`))
	assert.Equal(t, TrailerType(-1), t7)
	assert.Error(t, err)
}

func TestUnmarshalLowercase(t *testing.T) {
	x := &trailerWrapper{}
	in := []byte(`{"trailer": "lf"}`)
	err := json.Unmarshal(in, x)
	assert.Nil(t, err)
	assert.Equal(t, &trailerWrapper{Trailer: LF}, x)
}

func TestUnmarshalUnknown(t *testing.T) {
	x := &trailerWrapper{}
	in := []byte(`{"trailer": "UNK"}`)
	err := json.Unmarshal(in, x)
	assert.Error(t, err)
	assert.Equal(t, &trailerWrapper{Trailer: -1}, x)
}

func TestUnmarshal(t *testing.T) {
	x := &trailerWrapper{}
	in := []byte(`{"trailer": "NUL"}`)
	err := json.Unmarshal(in, x)
	assert.Nil(t, err)
	assert.Equal(t, &trailerWrapper{Trailer: NUL}, x)
}

func TestMarshalUnknown(t *testing.T) {
	res, err := json.Marshal(&trailerWrapper{Trailer: TrailerType(-2)})
	assert.Error(t, err)
	assert.Empty(t, res)
}

func TestMarshal(t *testing.T) {
	res, err := json.Marshal(&trailerWrapper{Trailer: NUL})
	assert.Nil(t, err)
	assert.Equal(t, `{"trailer":"NUL"}`, string(res))
}
