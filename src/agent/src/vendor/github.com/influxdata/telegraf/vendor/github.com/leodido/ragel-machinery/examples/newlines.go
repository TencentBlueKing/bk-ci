package examples

import (
	"fmt"
	parser "github.com/leodido/ragel-machinery/parser"
	"io"
)

const newlinesStart int = 1
const newlinesError int = 0

const newlinesEnMain int = 1

type newlinesMachine struct {
	// define here your support variables for ragel actions
	lines []string
}

// Exec implements the parser.Parser interface.
func (m *newlinesMachine) Exec(s *parser.State) (int, int) {
	// Tell it to parse from the start for each byte(10) delimited incoming chunk
	cs := 1
	// Retrieve previously stored parsing variables
	_, p, pe, eof, data := s.Get()
	// Inline FSM code here

	{
		if p == pe {
			goto _testEof
		}
		switch cs {
		case 1:
			goto stCase1
		case 0:
			goto stCase0
		case 2:
			goto stCase2
		case 3:
			goto stCase3
		case 4:
			goto stCase4
		case 5:
			goto stCase5
		case 6:
			goto stCase6
		case 7:
			goto stCase7
		case 8:
			goto stCase8
		case 9:
			goto stCase9
		case 10:
			goto stCase10
		case 11:
			goto stCase11
		}
		goto stOut
	stCase1:
		if data[p] == 101 {
			goto st2
		}
		goto st0
	stCase0:
	st0:
		cs = 0
		goto _out
	st2:
		if p++; p == pe {
			goto _testEof2
		}
	stCase2:
		if data[p] == 120 {
			goto st3
		}
		goto st0
	st3:
		if p++; p == pe {
			goto _testEof3
		}
	stCase3:
		if data[p] == 97 {
			goto st4
		}
		goto st0
	st4:
		if p++; p == pe {
			goto _testEof4
		}
	stCase4:
		if data[p] == 109 {
			goto st5
		}
		goto st0
	st5:
		if p++; p == pe {
			goto _testEof5
		}
	stCase5:
		if data[p] == 112 {
			goto st6
		}
		goto st0
	st6:
		if p++; p == pe {
			goto _testEof6
		}
	stCase6:
		if data[p] == 108 {
			goto st7
		}
		goto st0
	st7:
		if p++; p == pe {
			goto _testEof7
		}
	stCase7:
		if data[p] == 101 {
			goto st8
		}
		goto st0
	st8:
		if p++; p == pe {
			goto _testEof8
		}
	stCase8:
		if data[p] == 32 {
			goto st9
		}
		if 48 <= data[p] && data[p] <= 57 {
			goto tr9
		}
		goto st0
	st9:
		if p++; p == pe {
			goto _testEof9
		}
	stCase9:
		if 48 <= data[p] && data[p] <= 57 {
			goto tr9
		}
		goto st0
	tr9:

		{
			m.lines = append(m.lines, string(data[:p+1]))
		}

		goto st10
	st10:
		if p++; p == pe {
			goto _testEof10
		}
	stCase10:
		if data[p] == 10 {
			goto st11
		}
		if 48 <= data[p] && data[p] <= 57 {
			goto tr9
		}
		goto st0
	st11:
		if p++; p == pe {
			goto _testEof11
		}
	stCase11:
		goto st0
	stOut:
	_testEof2:
		cs = 2
		goto _testEof
	_testEof3:
		cs = 3
		goto _testEof
	_testEof4:
		cs = 4
		goto _testEof
	_testEof5:
		cs = 5
		goto _testEof
	_testEof6:
		cs = 6
		goto _testEof
	_testEof7:
		cs = 7
		goto _testEof
	_testEof8:
		cs = 8
		goto _testEof
	_testEof9:
		cs = 9
		goto _testEof
	_testEof10:
		cs = 10
		goto _testEof
	_testEof11:
		cs = 11
		goto _testEof

	_testEof:
		{
		}
	_out:
		{
		}
	}

	// Update parsing variables
	s.Set(cs, p, pe, eof)
	return p, pe
}

func (m *newlinesMachine) OnErr(c []byte) {
	fmt.Println("OnErr")
	if len(c) > 0 {
		fmt.Println(string(c))
	}
}

func (m *newlinesMachine) OnEOF(c []byte) {
	fmt.Println("OnEOF")
	if len(c) > 0 {
		fmt.Println(string(c))
	}
}

func (m *newlinesMachine) OnCompletion() {
	fmt.Println("OnCompletion")
}

// Parse composes a new ragel parser for the incoming stream using the current FSM.
func (m *newlinesMachine) Parse(r io.Reader) []string {
	m.lines = []string{}
	p := parser.New(
		parser.ArbitraryReader(r, '\n'), // How to read the stream
		m,                               // How to parse it
		parser.WithStart(1),             // Options
	)
	p.Parse()
	return m.lines
}
