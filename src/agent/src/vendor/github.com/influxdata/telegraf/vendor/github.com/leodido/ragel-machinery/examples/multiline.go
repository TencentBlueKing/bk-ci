package examples

import (
	"fmt"
	parser "github.com/leodido/ragel-machinery/parser"
	"io"
)

const multilineStart int = 1
const multilineError int = 0

const multilineEnMain int = 1

type multilineMachine struct {
	// define here your support variables for ragel actions
	item  []byte
	items []string
}

// Exec implements the ragel.Parser interface.
func (m *multilineMachine) Exec(s *parser.State) (int, int) {
	// Retrieve previously stored parsing variables
	cs, p, pe, eof, data := s.Get()
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
		}
		goto stOut
	stCase1:
		if data[p] == 36 {
			goto tr0
		}
		goto st0
	stCase0:
	st0:
		cs = 0
		goto _out
	tr0:

		if len(m.item) > 0 {
			m.items = append(m.items, string(m.item[:len(m.item)-1]))
		}
		// Initialize a new item
		m.item = make([]byte, 0)

		goto st2
	st2:
		if p++; p == pe {
			goto _testEof2
		}
	stCase2:
		if data[p] == 10 {
			goto st3
		}
		if 48 <= data[p] && data[p] <= 57 {
			goto st3
		}
		goto st0
	st3:
		if p++; p == pe {
			goto _testEof3
		}
	stCase3:
		if data[p] == 10 {
			goto tr3
		}
		if 48 <= data[p] && data[p] <= 57 {
			goto st3
		}
		goto st0
	tr3:

		// Collect data each trailer we encounter
		m.item = append(m.item, data...)

		goto st4
	st4:
		if p++; p == pe {
			goto _testEof4
		}
	stCase4:
		switch data[p] {
		case 10:
			goto tr3
		case 36:
			goto tr0
		}
		if 48 <= data[p] && data[p] <= 57 {
			goto st3
		}
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

func (m *multilineMachine) OnErr(c []byte) {
	fmt.Println("OnErr")
	if len(c) > 0 {
		fmt.Println(string(c))
	}
}

func (m *multilineMachine) OnEOF(c []byte) {
	fmt.Println("OnEOF")
	if len(c) > 0 {
		fmt.Println(string(c))
	}
}

func (m *multilineMachine) OnCompletion() {
	fmt.Println("OnCompletion")
	if len(m.item) > 0 {
		m.items = append(m.items, string(m.item))
	}
}

func (m *multilineMachine) Parse(r io.Reader) {
	m.items = []string{}
	p := parser.New(
		parser.ArbitraryReader(r, 10), // How to read the stream
		m,                             // How to parse it
		parser.WithStart(1),           // Options
	)
	p.Parse()
}
