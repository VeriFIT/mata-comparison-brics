package cz.verifit;

import cz.verifit.capnp.Afa.Model.Separated;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import org.capnproto.ListList;
import org.capnproto.PrimitiveList;
import org.capnproto.StructList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class RegexParser {
    private static void toRange16Nfa(Automaton aut, String fileName) throws IOException {
        org.capnproto.MessageBuilder message = new org.capnproto.MessageBuilder();
        Separated.Range16Nfa.Builder outnfa = message.initRoot(Separated.Range16Nfa.factory);

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

        outnfa.setInitial(stateToID.get(aut.getInitialState()));

        PrimitiveList.Int.Builder finals = outnfa.initFinals(finalStatesIDs.size());
        int f = 0;
        for (int fq: finalStatesIDs) { finals.set(f, fq); f++; }

        ListList.Builder<StructList.Builder<Separated.ConjunctR16Q.Builder>> transitions = outnfa.initStates(numOfStates);

        for (int i = 0; i < numOfStates; ++i) {
            State stateFrom = idToState.get(i);
            var transitionsFrom = stateFrom.getTransitions();
            StructList.Builder<Separated.ConjunctR16Q.Builder> nfaTransitionsFrom = transitions.init(i, transitionsFrom.size());

            int j = 0;
            for (var transition : transitionsFrom) {
                Separated.ConjunctR16Q.Builder conjunct = nfaTransitionsFrom.get(j);
                conjunct.setState(stateToID.get(transition.getDest()));
                StructList.Builder<Separated.Range16.Builder> outRanges = conjunct.initRanges(1);

                Separated.Range16.Builder outRange = outRanges.get(0);
                outRange.setBegin((short)(int)transition.getMin());
                outRange.setEnd((short)(int)transition.getMax());

                ++j;
            }
        }

        File outputFile = new File(fileName);
        outputFile.createNewFile(); // if file already exists will do nothing
        FileOutputStream outfile = new FileOutputStream(outputFile, false);
        WritableByteChannel outchan = outfile.getChannel();
        try {
            // System.out.println("WRITING");
            org.capnproto.Serialize.write(outchan, message);
            // System.out.println("WRITTEN");
        } finally {
            try {
                outchan.close();
            } finally {
                outfile.close();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Program expects one argument (name of file to print result to)");
            return;
        }

        Scanner input = new Scanner(System.in);
        String regex = input.nextLine();
        System.err.println("Parsing regex: " + regex);
        try {
            Automaton aut = (new RegExp(regex, RegExp.EMPTY)).toAutomaton(false);
            toRange16Nfa(aut, args[0]);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
