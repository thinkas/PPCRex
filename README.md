This project provides a test string generation tool for detecting semantic bugs in regular expressions. The tool converts a regular expression into its minimal deterministic finite automaton (DFA) and systematically generates test cases guided by Prime Path Coverage (PPC), a strong structural coverage criterion in software testing. To address the common problem of excessively large test suites, the tool incorporates a multi-stage reduction strategy that combines minimum-flow-based path selection, structural path clustering, and pairwise-testing.

The framework supports standard regular expression operators, including:

Concatenation Union (|) Kleene star (*) One-or-more (+) Optional (?) Bounded repetition ({m,n}) Character classes ([a-z], [0-9]) Predefined character classes (\d)

Note: Non-regular features such as backreferences and lookaround assertions are not supported.
