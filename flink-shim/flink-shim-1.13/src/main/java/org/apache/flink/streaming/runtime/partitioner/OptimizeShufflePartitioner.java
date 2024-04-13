/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.runtime.partitioner;

import org.apache.flink.runtime.io.network.api.writer.SubtaskStateMapper;
import org.apache.flink.runtime.plugable.SerializationDelegate;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import java.io.Serializable;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * Partitioner that distributes the data equally by selecting one output channel randomly.
 *
 * @param <T> Type of the Tuple
 */
public class OptimizeShufflePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    private final Random random = new Random();
    private RandomCollection randomCollection = new RandomCollection();

    @Override
    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        return randomCollection.next();
    }

    @Override
    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.ROUND_ROBIN;
    }

    @Override
    public StreamPartitioner<T> copy() {
        return new OptimizeShufflePartitioner<T>();
    }

    @Override
    public boolean isPointwise() {
        return false;
    }

    @Override
    public String toString() {
        return "OPTIMIZE_SHUFFLE";
    }

    public void setBufferStatista(Map<Integer, Integer> bufferStatista) {
        if (bufferStatista != null && bufferStatista.size() == numberOfChannels) {
            randomCollection = new RandomCollection();
            randomCollection.add(bufferStatista);
        }
    }

    private class RandomCollection implements Serializable {
        private transient NavigableMap<Integer, Integer> map;
        private Random random = new Random();
        private int weightTotal = 0;
        private int total = 0;

        public RandomCollection() {
            map = new TreeMap<>();
            random = new Random();
        }

        public RandomCollection add(Map<Integer, Integer> source) {
            if (map == null) {
                map = new TreeMap<>();
            }
            total = source.values().stream().reduce(Integer::sum).orElse(1);
            source.forEach((k, v) -> addWeight(total - v, k));
            return this;
        }

        private void addWeight(double weight, Integer result) {
            if (weight <= 0) return;
            weightTotal += weight;
            map.put(weightTotal, result);
        }

        public Integer next() {
            if (total == 0) {
                return random.nextInt(numberOfChannels);
            }
            int value = (int) (random.nextDouble() * weightTotal);
            return map.higherEntry(value).getValue();
        }
    }
}


