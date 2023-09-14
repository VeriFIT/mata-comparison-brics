package cz.verifit;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MataPrinter {
    private static void toMata(Automaton aut, String fileName) throws IOException {
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

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Program expects one argument (name of .range16nfa file)");
            return;
        }

        try {
            toMata(new EmpParser().readFromRange16Nfa(args[0]), args[0] + ".mata");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
