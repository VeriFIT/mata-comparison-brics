package cz.verifit;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.serialization.aut.AUTWriter;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class MataFormat {
    private HashMap<String, Integer> symbolToInt = new HashMap<>();
    public Alphabet<Integer> automatalibAlph = null;

    public CompactDFA<Integer> mataToAutomatalib(String fileName) throws IOException {
        FastNFA<Integer> aut = new FastNFA<>(automatalibAlph);
        var a  = aut.addState();

        // lambda that takes string and returns state for it
        var idToState = new HashMap<String, FastNFAState>();
        Function<String,FastNFAState> getStateFromId = stateId -> {
            if (!idToState.containsKey(stateId)) {
                idToState.put(stateId, aut.addState());
            }
            return idToState.get(stateId);
        };

        try (Scanner scanner = new Scanner(new File(fileName))) {
            if (!scanner.hasNextLine()) {
                throw new RuntimeException("first line of mata file should be type of automaton");
            } else {
                String automaton_definition = scanner.nextLine();
                if (!Objects.equals(automaton_definition, "@NFA-explicit")) {
                    throw new RuntimeException("automatalib can handle only explicit automata");
                }
            }

            while (scanner.hasNextLine()) {
                var tokens = scanner.nextLine().split("\\s+");
                if (tokens[0].charAt(0) == '%') {
                    if (tokens[0].equals("%Initial")) {
                        for (int i = 1; i < tokens.length; ++i) {
                            aut.setInitial(getStateFromId.apply(tokens[i]), true);
                        }
                    } else if (tokens[0].equals("%Final")) {
                        for (int i = 1; i < tokens.length; ++i) {
                            aut.setAccepting(getStateFromId.apply(tokens[i]), true);
                        }
                    }
                } else {
                    // transition
                    if (tokens.length != 3) {
                        throw new RuntimeException("transition was not given as 'state symbol state' (there might be whitespaces in symbol)");
                    }

                    aut.addTransition(
                            getStateFromId.apply(tokens[0]),
                            symbolToInt.get(tokens[1]),
                            getStateFromId.apply(tokens[2])
                    );
                }
            }
        }
//        System.out.println("NFA:");
//        GraphDOT.write(aut, automatalibAlph, System.out);
        var detAut = NFAs.determinize(aut);
//        System.out.println("DFA:");
//        GraphDOT.write(detAut, automatalibAlph, System.out);
        return detAut;
    }

    public void intializeExplicitAlphabet(List<String> fileNames) throws FileNotFoundException {
        for (String fileName : fileNames) {
            try (Scanner scanner = new Scanner(new File(fileName))) {
                while(scanner.hasNextLine()) {
                    var tokens= scanner.nextLine().split("\\s+");
                    if (tokens[0].charAt(0) == '@') {
                        if (!tokens[0].equals("@NFA-explicit")) {
                            throw new RuntimeException("symbols can only be parsed from explicit NFA");
                        }
                    } else if (tokens[0].charAt(0) != '%') {
                        String symbol = tokens[1];
                        if (!symbolToInt.containsKey(symbol)) {
                            symbolToInt.put(symbol, symbolToInt.size());
                        }
                    }
                }
            }
        }

        automatalibAlph = Alphabets.integers(0, symbolToInt.size() - 1);
    }

    // transform mata to brics format, if explicit automaton, expects the alphabet to be initialized using initializeExplicitAlphabet
    public Automaton mataToBrics(String fileName) throws IOException {
        // lambda that takes string and returns state for it
        var idToState = new HashMap<String, State>();
        Function<String,State> getStateFromId = stateId -> {
            if (!idToState.containsKey(stateId)) {
                idToState.put(stateId, new State());
            }
            return idToState.get(stateId);
        };

        Automaton aut = new Automaton();
        ArrayList<State> fakeInitialStates = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(fileName))) {
            Boolean isInterval = null; // if false -> explicit
            if (!scanner.hasNextLine()) {
                throw new RuntimeException("first line of mata file should be type of automaton");
            } else {
                String automaton_definition = scanner.nextLine();
                if (Objects.equals(automaton_definition, "@NFA-intervals")) {
                    isInterval = true;
                } else if (Objects.equals(automaton_definition, "@NFA-explicit")) {
                    isInterval = false;
                } else {
                    throw new RuntimeException("unknown type of automaton in mata file");
                }
            }

            while (scanner.hasNextLine()) {
                var tokens = scanner.nextLine().split("\\s+");
                if (tokens[0].charAt(0) == '%') {
                    if (tokens[0].equals("%Initial")) {
                        for (int i = 1; i < tokens.length; ++i) {
                            fakeInitialStates.add(getStateFromId.apply(tokens[i]));
                        }
                    } else if (tokens[0].equals("%Final")) {
                        for (int i = 1; i < tokens.length; ++i) {
                            getStateFromId.apply(tokens[i]).setAccept(true);
                        }
                    }
                } else {
                    // transition
                    if (tokens.length != 3) {
                        throw new RuntimeException("transition was not given as 'state symbol state' (there might be whitespaces in symbol)");
                    }

                    State stateFrom = getStateFromId.apply(tokens[0]);
                    State stateTo = getStateFromId.apply(tokens[2]);

                    if (isInterval) {
                        // interval is given as [<int>-<int>]
                        var interval = tokens[1].substring(1, tokens[1].length() - 1).split("-");
                        int intervalStart = Integer.parseInt(interval[0]);
                        int intervalEnd = Integer.parseInt(interval[1]);
                        stateFrom.addTransition(new Transition((char) intervalStart, (char) intervalEnd, stateTo));
                    } else {
                        int symbol = symbolToInt.get(tokens[1]);
                        stateFrom.addTransition(new Transition((char) symbol, stateTo));
                    }
                }
            }
        }

        if (fakeInitialStates.size() == 1) {
            aut.setInitialState(fakeInitialStates.get(0));
        } else {
            State initState = new State();
            for (State fakeInitState : fakeInitialStates) {
                if (fakeInitState.isAccept()) {
                    initState.setAccept(true);
                }
                for (Transition tran : fakeInitState.getTransitions()) {
                    initState.addTransition(tran);
                }
            }
            aut.setInitialState(initState);
        }

        aut.setDeterministic(false);
        aut.restoreInvariant();
        aut.reduce();
        return aut;
    }

    public static void bricsToMata(Automaton aut, String fileName) throws IOException {
        Map<State,Integer> stateToID = new HashMap<>();
        Map<Integer,State> idToState = new HashMap<>();

        var states = aut.getStates();

        ArrayList<Integer> finalStatesIDs = new ArrayList<>();
        int numOfStates = 0;
        for (State s : states) {
            stateToID.put(s, numOfStates);
            idToState.put(numOfStates, s);
            if (s.isAccept()) {
                finalStatesIDs.add(numOfStates);
            }
            ++numOfStates;
        }

        File outputFile = new File(fileName);
        outputFile.createNewFile(); // if file already exists will do nothing
        try(var fw = new FileWriter(outputFile)) {
            fw.append("@NFA-intervals\n")
                    .append("%Alphabet-numbers\n")
                    .append("%Initial q" + stateToID.get(aut.getInitialState()) + "\n")
                    .append("%Final");
            for (int fq : finalStatesIDs) {
                fw.append(" q" + fq);
            }
            fw.append("\n");
            for (int i = 0; i < numOfStates; ++i) {
                State stateFrom = idToState.get(i);
                var transitionsFrom = stateFrom.getTransitions();

                for (var transition : transitionsFrom) {
                    fw.append("q" + i + " [" + (int) transition.getMin() + "-" + (int) transition.getMax() + "] q" + stateToID.get(transition.getDest()) + "\n");
                }
            }
        }
    }
}
