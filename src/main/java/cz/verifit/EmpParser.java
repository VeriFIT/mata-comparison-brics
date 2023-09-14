package cz.verifit;

import cz.verifit.capnp.Afa.Model.Separated;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class EmpParser {
    private Map<Integer, Automaton> idToAutomaton = new HashMap<Integer, Automaton>();
    private String pathToAutomata = "gen_aut/";
    private int autNumToCheck = -1;
    private int autNumToCheck1 = -1;
    private int autNumToCheck2 = -1;
    private boolean alwaysMinimize = false;

    public Automaton readFromRange16Nfa(String fileName) throws IOException {
        org.capnproto.MessageReader message = org.capnproto.Serialize.read((new java.io.FileInputStream(fileName)).getChannel());
        Separated.Range16Nfa.Reader nfa = message.getRoot(Separated.Range16Nfa.factory);

        var idToState = new HashMap<Integer, State>();

        var transitions = nfa.getStates();
        var numOfStates = transitions.size();
        for (int stateFromId = 0; stateFromId < numOfStates; ++stateFromId) {
            idToState.put(stateFromId, new State());
        }

        for (int stateFromId = 0; stateFromId < numOfStates; ++stateFromId) {
            State stateFrom = idToState.get(stateFromId);
            for (var transitionsFromState : transitions.get(stateFromId)) {
                State stateTo = idToState.get(transitionsFromState.getState());
                for (var range : transitionsFromState.getRanges()) {
                    Transition trans = new Transition((char) range.getBegin(), (char) range.getEnd(), stateTo);
                    stateFrom.addTransition(trans);
                }
            }
        }

        for (int i = 0; i < nfa.getFinals().size(); ++i) {
            idToState.get(nfa.getFinals().get(i)).setAccept(true);
        }

        Automaton aut = new Automaton();
        aut.setInitialState(idToState.get(nfa.getInitial()));
        aut.setDeterministic(false);
        aut.restoreInvariant();
        if (alwaysMinimize) {
            aut.minimize();
        }
        return aut;
    }

    private int getAutNumFromName(String name)
    {
        if (!name.startsWith("aut"))
        {
            throw new RuntimeException("Automata names should be in form autN for some number N");
        }

        return Integer.parseInt(name.substring(3));
    }

    private void readLine(String line) throws IOException {
        var tokens = line.replaceAll("[)(=]", "").split("\\s+");
        if (tokens[0].equals("load_automaton"))
        {
            if (tokens.length != 2) {
                throw new RuntimeException("load_automaton expects exactly one automaton to load");
            }
            idToAutomaton.put(getAutNumFromName(tokens[1]), readFromRange16Nfa(Paths.get(pathToAutomata, tokens[1] + ".range16nfa").toString()));
        }
        else if (tokens[0].equals("is_empty"))
        {
            if (tokens.length != 2) {
                throw new RuntimeException("is_empty expects exactly one automaton to check for emptiness");
            }
            autNumToCheck = getAutNumFromName(tokens[1]);
        }
        else if (tokens[0].equals("incl")) {
            if (tokens.length != 3) {
                throw new RuntimeException("incl expects exactly two automata to check for inclusion");
            }
            autNumToCheck1 = getAutNumFromName(tokens[1]);
            autNumToCheck2 = getAutNumFromName(tokens[2]);
        }
        else {
            if (tokens.length < 3) {
                throw new RuntimeException("Reading operation with not enough arguments");
            }

            if (!idToAutomaton.containsKey(getAutNumFromName(tokens[2])))
            {
                throw new RuntimeException("Trying to apply operation on not already parsed/processed automaton");
            }
            Automaton result = idToAutomaton.get(getAutNumFromName(tokens[2]));

            if (tokens[1].equals("compl"))
            {
                idToAutomaton.put(getAutNumFromName(tokens[0]), result.complement());
            }
            else
            {
                for (int i = 3; i < tokens.length; i++)
                {
                    if (!idToAutomaton.containsKey(getAutNumFromName(tokens[i])))
                    {
                        throw new RuntimeException("Trying to apply operation on not already parsed/processed automaton");
                    }
                    Automaton operand = idToAutomaton.get(getAutNumFromName(tokens[i]));

                    if (tokens[1].equals("union"))
                    {
                        result = result.union(operand);
                    }
                    else if (tokens[1].equals("inter"))
                    {
                        result = result.intersection(operand);
                    }
                    else {
                        throw new RuntimeException("Unknown operation");
                    }
                }
                idToAutomaton.put(getAutNumFromName(tokens[0]), result);
            }
        }
    }

    public boolean parseAndCheckEmptiness(String fileName, boolean alwaysMinimize) throws IOException {
        Automaton.setMinimizeAlways(alwaysMinimize);
        this.alwaysMinimize = alwaysMinimize;

        File inputFile = new File(fileName);
        pathToAutomata = Paths.get(inputFile.getAbsoluteFile().getParent(), "gen_aut/").toString();
        Scanner scanner = new Scanner(inputFile);
        while (scanner.hasNextLine()) {
            readLine(scanner.nextLine());
        }

        if (autNumToCheck == -1) {
            if (autNumToCheck1 == -1 || autNumToCheck2 == -1) {
                throw new RuntimeException("No check for emptyness or inclusion found in emp file");
            }
            if (!idToAutomaton.containsKey(autNumToCheck1) || !idToAutomaton.containsKey(autNumToCheck2)) {
                throw new RuntimeException("Trying to check inclusion of undefined automata");
            }
            return idToAutomaton.get(autNumToCheck1).subsetOf(idToAutomaton.get(autNumToCheck2));
        } else {
            if (!idToAutomaton.containsKey(autNumToCheck)) {
                throw new RuntimeException("Trying to check emptiness of undefined automaton");
            }
            return idToAutomaton.get(autNumToCheck).isEmpty();
        }
    }
}
