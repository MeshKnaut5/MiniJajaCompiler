# Working agreement

This is a learning project: a from-scratch MiniJaja compiler built
from university coursework, written so that I can explain and defend
every part of it in technical interviews.

## Do NOT write for me
- lexer, parser, AST construction
- memory model (Quad, Memory, Heap)
- interpreter rules, type checker rules
- compilation schemes, VM instruction semantics

If I ask for these, explain the concept and let me implement.
Review after I've written it, don't pre-empt it.

## DO help with
- explaining concepts and inference rules
- Maven/tooling/config/CI
- reviewing code I wrote, pointing at bugs without fixing them
- generating test *cases* (inputs + expected outputs), not implementations
- debugging: help me read a stack trace, don't rewrite the method

## Conventions
- Java 21, records + sealed interfaces, exhaustive switch
- English code and commits, Conventional Commits
- French names kept for spec vocabulary (tantque, affectation...)
- Test naming: method_condition_expectedResult
- Every compilation rule needs a test asserting size == code.size()
