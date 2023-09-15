package cz.verifit;

import dk.brics.automaton.Automaton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class EmpParser {
    private Map<Integer, Automaton> idToAutomaton = new HashMap<>();
    private ArrayList<String> pathsToAutomata;
    private long startTime;

    void startTimer() {
        startTime = System.nanoTime();
    }

    void endTimer(String name) {
        long elapsedTime = System.nanoTime() - startTime;
        double elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println(name + ": " + elapsedTimeInSecond);
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
            int autNum = getAutNumFromName(tokens[1]);

            startTimer();
            idToAutomaton.put(autNum, MataFormat.mataToBrics(pathsToAutomata.get(autNum-1)));
            endTimer("construction");
        }
        else if (tokens[0].equals("load_automata"))
        {
            for (int i = 0; i < pathsToAutomata.size(); ++i) {
                startTimer();
                idToAutomaton.put(i+1, MataFormat.mataToBrics(pathsToAutomata.get(i)));
                endTimer("construction");
            }
        }
        else if (tokens[0].equals("is_empty"))
        {
            if (tokens.length != 2) {
                throw new RuntimeException("is_empty expects exactly one automaton to check for emptiness");
            }
            startTimer();
            Boolean is_empty = idToAutomaton.get(getAutNumFromName(tokens[1])).isEmpty();
            endTimer("emptiness_check");
            System.out.println("emptiness_result: " + is_empty);
        }
        else if (tokens[0].equals("incl")) {
            if (tokens.length != 3) {
                throw new RuntimeException("incl expects exactly two automata to check for inclusion");
            }
            int aut1 = getAutNumFromName(tokens[1]);
            int aut2 = getAutNumFromName(tokens[2]);
            startTimer();
            Boolean is_included = idToAutomaton.get(aut1).subsetOf(idToAutomaton.get(aut2));
            endTimer("inclusion_check");
            System.out.println("inclusion_result: " + is_included);
        }
        else if (tokens[1].equals("interall"))
        {
            Automaton result = null;
            startTimer();
            for (Automaton aut : idToAutomaton.values()) {
                if (result == null) {
                    result = aut;
                } else {
                    result = result.intersection(aut);
                }
            }
            endTimer("interall");
            idToAutomaton.put(getAutNumFromName(tokens[0]), result);
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
                startTimer();
                idToAutomaton.put(getAutNumFromName(tokens[0]), result.complement());
                endTimer("compl");
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
                        startTimer();
                        result = result.union(operand);
                        endTimer("uni");
                    }
                    else if (tokens[1].equals("inter"))
                    {
                        startTimer();
                        result = result.intersection(operand);
                        endTimer("intersection");
                    }
                    else {
                        throw new RuntimeException("Unknown operation");
                    }
                }
                idToAutomaton.put(getAutNumFromName(tokens[0]), result);
            }
        }
    }

    public void parseAndInterpret(String fileName, ArrayList<String> pathsToAutomata) throws IOException {
        this.pathsToAutomata = pathsToAutomata;
        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                readLine(scanner.nextLine());
            }
        }
    }
}
