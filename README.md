# Comparison of Java automata libraries
Tool for comparing operations over automata in libraries [dk.brics.automaton](https://github.com/cs-au-dk/dk.brics.automaton) and [automatalib](https://github.com/LearnLib/automatalib).
It loads automata in `.mata` format and reads automata operations defined in `.emp` file, calls these operations in given library and outputs the times for each operation.

Compile using Maven:
```
mvn clean package
```

To run brics:
```
java -jar target/brics-emp-interpreter.jar path/to/file.emp path/to/automaton1.mata [path/to/automaton2.mata ...]
```
Both explicit and interval automata can be used for brics.

To run automatalib:
```
java -jar target/automatalib-emp-interpreter.jar path/to/file.emp path/to/automaton1.mata [path/to/automaton2.mata ...]
```
Only explicit automata can be used for automatalib.
