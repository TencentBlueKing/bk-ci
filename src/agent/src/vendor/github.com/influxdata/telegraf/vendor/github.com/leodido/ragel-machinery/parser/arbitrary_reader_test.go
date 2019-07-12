package parser

import (
	"fmt"
	"github.com/google/go-cmp/cmp"
	"io"
	"strings"
	"testing"
)

type result struct {
	data        string // the current data at each iteration
	num         int    // the number of bytes read
	forwardErr  error
	backwardErr error
	sought      int // the number of bytes sought in case of backwards seek
}

func TestArbitraryReader(t *testing.T) {
	for _, test := range []struct {
		input  string
		backto byte
		update bool
		res    []result
	}{
		{
			input: "ciao",
			res: []result{
				{"ciao", 4, io.ErrUnexpectedEOF, nil, 0},
			},
		},
		{
			input: "ciao\nmondo",
			res: []result{
				{"ciao\n", 5, nil, nil, 0},
				{"ciao\nmondo", 5, io.ErrUnexpectedEOF, nil, 0},
			},
		},
		{
			input: "ciao\nmondo\n",
			res: []result{
				{"ciao\n", 5, nil, nil, 0},
				{"ciao\nmondo\n", 6, nil, nil, 0},
				{"ciao\nmondo\n", 0, io.EOF, nil, 0},
			},
		},
		// Updating the window
		{
			input:  "ciao\nmondo",
			update: true,
			res: []result{
				{"ciao\n", 5, nil, nil, 0},
				{"mondo", 5, io.ErrUnexpectedEOF, nil, 0},
			},
		},
		{
			input:  "ciao\nmondo\n",
			update: true,
			res: []result{
				{"ciao\n", 5, nil, nil, 0},
				{"mondo\n", 6, nil, nil, 0},
				{"", 0, io.EOF, nil, 0},
			},
		},
		{
			input:  "abcabc\ncbacba\n",
			backto: 'b',  // and then seek backwards until this
			update: true, // so to start again from the character after its (backto) first occurrence
			res: []result{
				{"abcabc\n", 7, nil, nil, 3},
				{"c\ncbacba\n", 7, nil, nil, 3}, // 9 - 2 (already read)
				{"a\n", 0, io.EOF, nil, 0},
			},
		},
		{
			input:  "ciao\nciao\n",
			backto: '\n',
			res: []result{
				{"ciao\n", 5, nil, nil, 1},
				{"ciao\nciao\n", 5, nil, nil, 1},
			},
		},
		{
			input:  "ciao\nciao",
			backto: '\n',
			res: []result{
				{"ciao\n", 5, nil, nil, 1},
				{"ciao\nciao", 4, io.ErrUnexpectedEOF, nil, 0}, // not going backwards since we avoid loop checking for error
			},
		},
	} {
		r := ArbitraryReader(strings.NewReader(test.input), '\n')

		for _, o := range test.res {
			res, err := r.Read()
			// Check error reading forward
			errorCheck(t, o.forwardErr, err)

			if test.backto > 0 && err == nil {
				nb, err := r.Seek(test.backto, true)
				// Check error reading backwards
				errorCheck(t, o.backwardErr, err)

				// Check number of sought bytes
				check(t, o.sought, nb)
			}

			// Check number of read bytes
			check(t, o.num, len(res))

			// Check the current data window
			check(t, o.data, string(r.data))

			if test.update {
				// Updating the start and end position of the window for next run
				r.p = r.pe
			}
		}
	}
}

func check(t *testing.T, x, y interface{}) {
	if diff := cmp.Diff(x, y); diff != "" {
		t.Errorf("(-want +got)\n%s", diff)
	}
}

func errorCheck(t *testing.T, x, y error) {
	if x != nil && y != nil && x.Error() != y.Error() {
		t.Errorf("(-want +got)\n%s", fmt.Sprintf("-: %#v\n+: %#v", x.Error(), y.Error()))
	}
}
