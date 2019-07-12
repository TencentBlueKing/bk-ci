package examples

import (
    "io"
    "fmt"
    parser "github.com/leodido/ragel-machinery/parser"
)

%%{
machine multiline;

# unsigned alphabet
alphtype uint8;

action on_trailer {
    // Collect data each trailer we encounter
    m.item = append(m.item, data...)
}

action on_init {
    if len(m.item) > 0 {
        m.items = append(m.items, string(m.item[:len(m.item)-1]))
    }
    // Initialize a new item
    m.item = make([]byte, 0)
}

t = 10;

main := 
    start: (
        # Note that corpus can contain trailers - ie., can span more lines in this case.
        '$' >on_init (digit | t)+ -> trailer
    ),
    trailer: (
        t >on_trailer -> final |
        t >on_trailer -> start 
    );
}%%

%% write data nofinal;

type multilineMachine struct{
    // define here your support variables for ragel actions
    item []byte
    items []string
}

// Exec implements the ragel.Parser interface.
func (m *multilineMachine) Exec(s *parser.State) (int, int) {
    // Retrieve previously stored parsing variables
    cs, p, pe, eof, data := s.Get()
    // Inline FSM code here
    %% write exec;
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
        parser.ArbitraryReader(r, 10),          // How to read the stream
        m,                                      // How to parse it
        parser.WithStart(%%{ write start; }%%), // Options    
    )
    p.Parse()
}