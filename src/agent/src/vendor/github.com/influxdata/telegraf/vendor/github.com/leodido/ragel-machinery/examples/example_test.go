package examples

import (
	"fmt"
	"strings"
)

func Example_newlines() {
	in := `example 0
example 1
example 2
example X
`

	results := (&newlinesMachine{}).Parse(strings.NewReader(in))

	for _, elem := range results {
		fmt.Println("RECV", elem)
	}
	// Output:
	// OnEOF
	// OnCompletion
	// RECV example 0
	// RECV example 1
	// RECV example 2
}

func Example_multiline() {
	in := `$1
2
$3
`
	fsm := &multilineMachine{}
	fsm.Parse(strings.NewReader(in))
	for _, item := range fsm.items {
		fmt.Println("RECV", item)
	}
	// Output:
	// OnEOF
	// OnCompletion
	// RECV $1
	// 2
	// RECV $3
}

func Example_multiline_err() {
	in := `$1`
	fsm := &multilineMachine{}
	fsm.Parse(strings.NewReader(in))
	for _, item := range fsm.items {
		fmt.Println("RECV", item)
	}
	// Output:
	// OnErr
	// $1
	// OnCompletion
}
