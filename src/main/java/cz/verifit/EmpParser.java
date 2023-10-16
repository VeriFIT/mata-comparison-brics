package cz.verifit;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization;
import net.automatalib.util.minimizer.Minimizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EmpParser {
    private Map<Integer, Automaton> idToAutomatonBrics = new HashMap<>();
    private Map<Integer, CompactDFA<Integer>> idToAutomatonAutomatalib = new HashMap<>();
    private ArrayList<Automaton> bricsAutomata = new ArrayList<>();
    private ArrayList<FastNFA<Integer>> automatalibAutomata = new ArrayList<>();
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

            if (automatalib) {
                startTimer();
                idToAutomatonAutomatalib.put(autNum, NFAs.determinize(automatalibAutomata.get(0)));
                endTimer("determinize");
                automatalibAutomata.remove(0);
            } else {
                idToAutomatonBrics.put(autNum, bricsAutomata.get(0));
                bricsAutomata.remove(0);
            }
        }
        else if (tokens[0].equals("load_automata"))
        {
            if (automatalib) {
                for (int i = 0; i < automatalibAutomata.size(); ++i) {
                    startTimer();
                    idToAutomatonAutomatalib.put(i + 1, NFAs.determinize(automatalibAutomata.get(i)));
                    endTimer("determinize");
                }
                automatalibAutomata.clear();
            } else {
                for (int i = 0; i < bricsAutomata.size(); ++i) {
                    idToAutomatonBrics.put(i+1, bricsAutomata.get(i));
                }
                bricsAutomata.clear();
            }
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
            if (automatalib) {
                startTimer();
                CompactDFA<Integer> result = null;
                for (var aut : idToAutomatonAutomatalib.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = DFAs.and(result, aut, parser.automatalibAlph);
                    }
                }
                idToAutomatonAutomatalib.put(getAutNumFromName(tokens[0]), result);
                endTimer("interall");
            } else {
                startTimer();
                Automaton result = null;
                for (Automaton aut : idToAutomatonBrics.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = result.intersection(aut);
                    }
                }
                idToAutomatonBrics.put(getAutNumFromName(tokens[0]), result);
                endTimer("interall");
            }
        }
        else if (tokens[1].equals("unionall"))
        {
            if (automatalib) {
                startTimer();
                CompactDFA<Integer> result = null;
                for (var aut : idToAutomatonAutomatalib.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = DFAs.or(result, aut, parser.automatalibAlph);
                    }
                }
                idToAutomatonAutomatalib.put(getAutNumFromName(tokens[0]), result);
                endTimer("uni");
            } else {
                startTimer();
                Automaton result = null;
                for (Automaton aut : idToAutomatonBrics.values()) {
                    if (result == null) {
                        result = aut;
                    } else {
                        result = result.union(aut);
                    }
                }
                idToAutomatonBrics.put(getAutNumFromName(tokens[0]), result);
                endTimer("uni");
            }
        }
        else if (tokens[1].equals("concat"))
        {
            if (automatalib) {
                throw new RuntimeException("Automatalib cannot do concatenation");
            } else {
                startTimer();
                var res = BasicOperations.concatenate(Arrays.stream(tokens).skip(2).map(aut -> idToAutomatonBrics.get(getAutNumFromName(aut))).toList());
                endTimer("concat");
                idToAutomatonBrics.put(getAutNumFromName(tokens[0]), res);
            }
        }
        else
        {
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
        long overallStartTime = System.nanoTime();

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

        for (var path : pathsToAutomata) {
            startTimer();
            if (automatalib) {
                automatalibAutomata.add(parser.mataToAutomatalib(path));
            } else {
                bricsAutomata.add(parser.mataToBrics(path));
            }
            endTimer("construction");
        }

        long empStartTime = System.nanoTime();

        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                readLine(scanner.nextLine());
            }
        }

        long elapsedTime = System.nanoTime() - empStartTime;
        double elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println("interpretation: " + elapsedTimeInSecond);

        elapsedTime = System.nanoTime() - overallStartTime;
        elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println("overall: " + elapsedTimeInSecond);
    }
}
