/* Copyright (C) 2013-2023 TU Dortmund
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
package net.automatalib.incremental.mealy.dag;

import net.automatalib.common.smartcollection.ResizingArrayStorage;

/**
 * A state in the DAG internally used by {@link IncrementalMealyDAGBuilder}.
 *
 * @param <O>
 *         output symbol type
 */
final class State<O> {

    private final StateSignature<O> signature;
    private int numIncoming;

    State(StateSignature<O> signature) {
        this.signature = signature;
    }

    void increaseIncoming() {
        numIncoming++;
    }

    void decreaseIncoming() {
        numIncoming--;
    }

    boolean isConfluence() {
        return numIncoming > 1;
    }

    State<O> getSuccessor(int idx) {
        return signature.successors.array[idx];
    }

    O getOutput(int idx) {
        return signature.outputs.array[idx];
    }

    StateSignature<O> getSignature() {
        return signature;
    }

    /**
     * See {@link ResizingArrayStorage#ensureCapacity(int)}.
     */
    void ensureInputCapacity(int capacity) {
        signature.successors.ensureCapacity(capacity);
        signature.outputs.ensureCapacity(capacity);
    }
}
