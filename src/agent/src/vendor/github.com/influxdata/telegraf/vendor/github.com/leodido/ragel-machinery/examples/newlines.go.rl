package examples

import (
    "io"
    "fmt"
    parser "github.com/leodido/ragel-machinery/parser"
)

%%{
machine newlines;

action line {
    {
        m.lines = append(m.lines, string(data[:p+1]))
    }
}

main := ("example"  . ' '? . digit+ @line) . 10;
}%%

%% write data nofinal;

type newlinesMachine struct{
    // define here your support variables for ragel actions
    lines []string
}

// Exec implements the parser.Parser interface.
func (m *newlinesMachine) Exec(s *parser.State) (int, int) {
    // Tell it to parse from the start for each byte(10) delimited incoming chunk 
    cs := %%{ write start; }%%
    // Retrieve previously stored parsing variables
    _, p, pe, eof, data := s.Get()
    // Inline FSM code here
    %% write exec;
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
        parser.ArbitraryReader(r, '\n'),        // How to read the stream
        m,                                      // How to parse it
        parser.WithStart(%%{ write start; }%%), // Options
    )
    p.Parse()
    return m.lines
}
