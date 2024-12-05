/* Copyright (C) 2013-2024 TU Dortmund University
 * This file is part of AutomataLib, http://www.automatalib.net/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.serialization.ba;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactNFA;
import net.automatalib.common.util.IOUtil;
import net.automatalib.exception.FormatException;
import net.automatalib.serialization.InputModelData;
import net.automatalib.serialization.InputModelDeserializer;

/**
 * Facade for BA parsing. For further information about the BA format, see <a
 * href="https://languageinclusion.org/doku.php?id=tools#the_ba_format">
 * https://languageinclusion.org/doku.php?id=tools#the_ba_format</a>.
 */
public class BAParser<I> implements
                InputModelDeserializer<String, CompactNFA<String>> {
    private static final Pattern transSplit = Pattern.compile("[,\\->]");
    private int stateSize = 0;
    private Set<String> alphabet =  new HashSet<>();
    private final List<Integer> initialStates;
    private final BitSet finalStates;
    private final Map<Integer,Map<String, Set<Integer>>> transitions; // state->symbol->state
    private boolean inInitialSection;
    private boolean inTransitionsSection;

    @Override
    public InputModelData<String, CompactNFA<String>> readModel(InputStream is) throws IOException, FormatException {
        final CompactNFA<String> automaton = readGenericNFA(is);
        return new InputModelData<>(automaton, automaton.getInputAlphabet());
    }

    public BAParser() {
        initialStates = new ArrayList<>();
        finalStates = new BitSet();
        transitions = new HashMap<>();
        inInitialSection = true;
        inTransitionsSection = true;
    }


    public CompactNFA<String> readGenericNFA(InputStream is) {
        parseLines(IOUtil.asNonClosingUTF8Reader(is));
        Alphabet<String> alph = Alphabets.fromCollections(alphabet);
        CompactNFA<String> result = new CompactNFA<>(alph, stateSize);
        for(int i=0;i<stateSize;i++) {
            if (initialStates.contains(i)) {
                result.addInitialState(finalStates.get(i));
            } else {
                result.addState(finalStates.get(i));
            }
        }
        for(int stateSrc=0;stateSrc<stateSize;stateSrc++) {
            Map<String, Set<Integer>> transStateSrc = transitions.get(stateSrc);
            for(Map.Entry<String, Set<Integer>> entry: transStateSrc.entrySet()) {
                result.addTransitions(stateSrc, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void parseLines(Reader r) {
        try (BufferedReader input = new BufferedReader(r)) {
            parseBufferedLines(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void parseBufferedLines(BufferedReader input) throws IOException {
        while (input.ready()) {
            final String line = input.readLine();
            parseLine(line);
        }
        if (finalStates.isEmpty()) {
            // In this case, all states are marked as final
            for(int i=0;i<stateSize;i++) {
                finalStates.set(i);
            }
        }
    }

    /**
     * Initial state(s), followed by transitions, followed by (optional) final states.
     * If no final states, then all states are accepting.
     * transition alphabet is numeric, but may need to be renumbered.
     * we'll also renumber states, just in case.
     */
    private void parseLine(String line) {
        // Lines starting with % are comments and ignored.
        if (line.charAt(0) == '%') {
            return;
        }
        if (inInitialSection) {
            if (line.indexOf(',') < 0) {
                int foundState = parseState(line);
                initialStates.add(foundState);
                return;
            }
        }
        if (inTransitionsSection) {
            String[] ss = transSplit.split(line);
            if (ss.length != 1 && ss.length != 4) {
                throw new RuntimeException("Unexpected BA line: " + line);
            }
            if (ss.length == 1) {
                // done with transitions, in final states
                inTransitionsSection = false;
            } else {
                inInitialSection = false; // encountered a transition
                // ss = {symbol, state1, ->, state2}
                String symbol = parseSymbol(ss[0]);
                int state1 = parseState(ss[1]);
                int state2 = parseState(ss[3]);
                Map<String,Set<Integer>> transMap = transitions.get(state1);
                if (transMap == null) {
                    transMap = new HashMap<>();
                    transitions.put(state1, transMap);
                }
                Set<Integer> transDest = transMap.get(symbol);
                if (transDest == null) {
                    transDest = new HashSet<>();
                    transMap.put(symbol, transDest);
                }
                transDest.add(state2);
                return;
            }
        }
        // in final states
        int finalState = parseState(line);
        finalStates.set(finalState);
    }

    private int parseState(String line) {
        int state = Integer.parseInt(line);
        stateSize = Math.max(stateSize, 1 + state);
        return state;
    }

    private String parseSymbol(String line) {
        alphabet.add(line);
        return line;
    }
}
