package cz.verifit;

import java.util.ArrayList;
import java.util.Arrays;

public class Automatalib {

    public static void main(String[] args) {
        String input;
        ArrayList<String> pathsToAutomata;
        if (args.length >= 2) {
            input = args[0];
            pathsToAutomata = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
        } else {
            System.err.println("error: Program expects at least one argument: path to .emp file and paths to automata");
            System.exit(1);
        }

        try {
            (new EmpParser()).parseAndInterpret(input, pathsToAutomata, true);
        } catch (Exception ex) {
            System.err.println("error: " + ex.getMessage());
            System.exit(1);
        }
    }
}
