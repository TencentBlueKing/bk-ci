package rfc5424

import (
	"testing"

	"github.com/influxdata/go-syslog"
	"github.com/stretchr/testify/assert"
)

func TestParserBestEffortOption(t *testing.T) {
	p1 := NewParser().(syslog.BestEfforter)
	assert.False(t, p1.HasBestEffort())

	p2 := NewParser(WithBestEffort()).(syslog.BestEfforter)
	assert.True(t, p2.HasBestEffort())
}

func TestParserParse(t *testing.T) {
	p := NewParser()
	pBest := NewParser(WithBestEffort())
	for _, tc := range testCases {
		tc := tc
		t.Run(rxpad(string(tc.input), 50), func(t *testing.T) {
			t.Parallel()

			message, merr := p.Parse(tc.input)
			partial, perr := pBest.Parse(tc.input)

			if !tc.valid {
				assert.Nil(t, message)
				assert.Error(t, merr)
				assert.EqualError(t, merr, tc.errorString)

				assert.Equal(t, tc.partialValue, partial)
				assert.EqualError(t, perr, tc.errorString)
			}
			if tc.valid {
				assert.Nil(t, merr)
				assert.NotEmpty(t, message)
				assert.Equal(t, message, partial)
				assert.Equal(t, merr, perr)
			}

			assert.Equal(t, tc.value, message)
		})
	}
}
