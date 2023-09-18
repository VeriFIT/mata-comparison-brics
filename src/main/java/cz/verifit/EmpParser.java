package cz.verifit;

import dk.brics.automaton.Automaton;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization;
import net.automatalib.util.minimizer.Minimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class EmpParser {
    private Map<Integer, Automaton> idToAutomatonBrics = new HashMap<>();
    private Map<Integer, CompactDFA<Integer>> idToAutomatonAutomatalib = new HashMap<>();
    private ArrayList<String> pathsToAutomata;
    private long startTime;
    private MataFormat parser = new MataFormat();
    Boolean explicit;
    Boolean automatalib;

    void startTimer() {
        startTime = System.nanoTime();
    }

    void endTimer(String name) {
        long elapsedTime = System.nanoTime() - startTime;
        double elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println(name + ": " + elapsedTimeInSecond);
    }

    void saveAut(int i, Automaton bricsAut) {
        idToAutomatonBrics.put(i, bricsAut);
    }

    void saveAut(int i, CompactDFA<Integer> automatalibAut) {
        idToAutomatonAutomatalib.put(i, automatalibAut);
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
            if (automatalib) {
                idToAutomatonAutomatalib.put(autNum, parser.mataToAutomatalib(pathsToAutomata.get(0)));

            } else {
                idToAutomatonBrics.put(autNum, parser.mataToBrics(pathsToAutomata.get(0)));
            }
            endTimer("construction");
            pathsToAutomata.remove(0);
        }
        else if (tokens[0].equals("load_automata"))
        {
            for (int i = 0; i < pathsToAutomata.size(); ++i) {
                startTimer();
                if (automatalib) {
                    idToAutomatonAutomatalib.put(idToAutomatonAutomatalib.size(), parser.mataToAutomatalib(pathsToAutomata.get(i)));
                } else {
                    idToAutomatonBrics.put(idToAutomatonBrics.size(), parser.mataToBrics(pathsToAutomata.get(i)));
                }
                endTimer("construction");
            }
            pathsToAutomata.clear();
        }
        else if (tokens[0].equals("is_empty"))
        {
            if (tokens.length != 2) {
                throw new RuntimeException("is_empty expects exactly one automaton to check for emptiness");
            }
            startTimer();
            final boolean is_empty;
            if (automatalib) {
                is_empty = DFAs.acceptsEmptyLanguage(idToAutomatonAutomatalib.get(getAutNumFromName(tokens[1])));
            } else {
                is_empty = idToAutomatonBrics.get(getAutNumFromName(tokens[1])).isEmpty();
            }
            endTimer("emptiness_check");
            System.out.println("emptiness_result: " + is_empty);
        }
        else if (tokens[0].equals("incl")) {
            if (tokens.length != 3) {
                throw new RuntimeException("incl expects exactly two automata to check for inclusion");
            }
            int aut1 = getAutNumFromName(tokens[1]);
            int aut2 = getAutNumFromName(tokens[2]);
            final boolean is_included;
            startTimer();
            if (automatalib) {
                var incl = DFAs.combine(idToAutomatonAutomatalib.get(aut1), idToAutomatonAutomatalib.get(aut2), parser.automatalibAlph, (a1, a2) -> a1 && !a2);
                is_included = DFAs.acceptsEmptyLanguage(incl);
            } else {
                is_included = idToAutomatonBrics.get(aut1).subsetOf(idToAutomatonBrics.get(aut2));
            }
            endTimer("inclusion_check");
            System.out.println("inclusion_result: " + is_included);
        }
        else if (tokens[1].equals("interall"))
        {
            startTimer();
            if (automatalib) {
                CompactDFA<Integer> result = null;
                for (var aut : idToAutomatonAutomatalib.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = DFAs.and(result, aut, parser.automatalibAlph);
                    }
                }
                idToAutomatonAutomatalib.put(getAutNumFromName(tokens[0]), result);
            } else {
                Automaton result = null;
                for (Automaton aut : idToAutomatonBrics.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = result.intersection(aut);
                    }
                }
                idToAutomatonBrics.put(getAutNumFromName(tokens[0]), result);
            }
            endTimer("interall");
        }
        else {
            if (tokens.length < 3) {
                throw new RuntimeException("Reading operation with not enough arguments");
            }

            var first_operand_id = getAutNumFromName(tokens[2]);

            if ((automatalib && !idToAutomatonAutomatalib.containsKey(first_operand_id)) || (!automatalib && !idToAutomatonBrics.containsKey(first_operand_id)))
            {
                throw new RuntimeException("Trying to apply operation on not already parsed/processed automaton");
            }
            Automaton resultBrics = null;
            CompactDFA<Integer> resultAutomatalib = null;
            if (automatalib) {
                resultAutomatalib = idToAutomatonAutomatalib.get(first_operand_id);
            } else {
                resultBrics = idToAutomatonBrics.get(first_operand_id);
            }

            if (tokens[1].equals("compl"))
            {
                startTimer();
                if (automatalib) {
                    idToAutomatonAutomatalib.put(getAutNumFromName(tokens[0]), DFAs.complement(resultAutomatalib, parser.automatalibAlph));
                } else {
                    idToAutomatonBrics.put(getAutNumFromName(tokens[0]), resultBrics.complement());
                }
                endTimer("compl");
            }
            else
            {
                for (int i = 3; i < tokens.length; i++)
                {
                    var operand_id = getAutNumFromName(tokens[i]);
                    if ((automatalib && !idToAutomatonAutomatalib.containsKey(operand_id)) || (!automatalib && !idToAutomatonBrics.containsKey(operand_id)))
                    {
                        throw new RuntimeException("Trying to apply operation on not already parsed/processed automaton");
                    }

                    if (tokens[1].equals("union"))
                    {
                        startTimer();
                        if (automatalib) {
                            resultAutomatalib = DFAs.or(resultAutomatalib, idToAutomatonAutomatalib.get(operand_id), parser.automatalibAlph);
                        } else {
                            resultBrics = resultBrics.union(idToAutomatonBrics.get(operand_id));
                        }
                        endTimer("uni");
                    }
                    else if (tokens[1].equals("inter"))
                    {
                        startTimer();
                        if (automatalib) {
                            resultAutomatalib = DFAs.and(resultAutomatalib, idToAutomatonAutomatalib.get(operand_id), parser.automatalibAlph);
                        } else {
                            resultBrics = resultBrics.intersection(idToAutomatonBrics.get(operand_id));
                        }
                        endTimer("intersection");
                    }
                    else {
                        throw new RuntimeException("Unknown operation");
                    }
                }
                if (automatalib) {
                    idToAutomatonAutomatalib.put(getAutNumFromName(tokens[0]), resultAutomatalib);
                } else {
                    idToAutomatonBrics.put(getAutNumFromName(tokens[0]), resultBrics);
                }
            }
        }
    }

    public void parseAndInterpret(String fileName, ArrayList<String> pathsToAutomata, Boolean automatalib) throws IOException {
        this.pathsToAutomata = pathsToAutomata;
        this.automatalib = automatalib;
        try (var r = new BufferedReader(new FileReader(pathsToAutomata.get(0)))) {
            if (r.readLine().equals("@NFA-explicit")) {
                explicit = true;
            } else {
                explicit = false;
            }
        }

        if (explicit) {
            parser.intializeExplicitAlphabet(pathsToAutomata);
        } else if (automatalib) {
            throw new RuntimeException("automatalib can only parse explicit automata");
        }

        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                readLine(scanner.nextLine());
            }
        }
    }
}
