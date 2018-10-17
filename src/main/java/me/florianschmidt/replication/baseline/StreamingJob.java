package me.florianschmidt.replication.baseline;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.collect.Lists;
import me.florianschmidt.replication.baseline.model.Feature;
import me.florianschmidt.replication.baseline.model.FraudDetectionResult;
import me.florianschmidt.replication.baseline.model.Transaction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.runtime.state.AbstractStateBackend;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.heap.HeapKeyedStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SocketTextStreamFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.TernaryBoolean;

import java.util.List;
import java.util.UUID;


public class StreamingJob {

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.enableCheckpointing(Time.minutes(1).toMilliseconds());

		env.setParallelism(1);

		StateBackend backend = new MemoryStateBackend(); // new FsStateBackend("file:///tmp/flink-docker/");
//		 backend = new RocksDBStateBackend(fileBackend, TernaryBoolean.TRUE);

		// disable latency tracking
		env.getConfig().setLatencyTrackingInterval(-1);

		env.setStateBackend(backend);

		env
				.addSource(new TransactionSource())
				.name("Transaction Generator")
				.uid("TransactionGenerator")
				.process(new ProcessFunction<Transaction, Transaction>() {
					@Override
					public void processElement(
							Transaction value, Context ctx, Collector<Transaction> out
					) {
						out.collect(value);
					}
				})
				// .disableChaining()
				.addSink(new TimeMeasureSink());

		/*		.map(new AddUuid())
				.name("Add UUID")
				.uid("AddUUID");

		DataStream<Feature> customerSpeed = transactions
				.keyBy(transaction -> transaction.customerId)
				.map(new CalculateSpeed())
				.name("Calculate Speed")
				.uid("CalculateSpeed");

		DataStream<Feature> amountDifferenceFromMean = transactions
				.keyBy(transaction -> transaction.cardId)
				.map(new CalculateDifferenceFromMean())
				.name("Calculate difference from mean")
				.uid("CalculateDifferenceFromMean");

		customerSpeed
				.connect(amountDifferenceFromMean)
				.keyBy(feature -> feature.transaction.uuid, feature -> feature.transaction.uuid)
				.flatMap(new JoinFeatures())
				.name("Join Features")
				.uid("JoinFeatures")

				.map(new FraudDetection())
				.uid("FraudDetectionOperator")
				.name("Fraud Detection")

				.addSink(new TimeMeasureSink())
				.uid("MeasuringLatencySink")
				.name("Measuring Latency Sink");*/


		env.execute("Fraud detection example");
	}

	public static class CalculateDifferenceFromMean extends RichMapFunction<Transaction, Feature> {

		private ListState<Transaction> lastTransactions;

		@Override
		public void open(Configuration parameters) throws Exception {
			ListStateDescriptor<Transaction> listStateDescriptor = new ListStateDescriptor<>(
					"last-n-transactions-per-card",
					Transaction.class
			);
			this.lastTransactions = getRuntimeContext().getListState(listStateDescriptor);
		}

		@Override
		public Feature map(Transaction transaction) throws Exception {
			List<Transaction> transactions = Lists.newArrayList(lastTransactions.get().iterator());
			double mean = mean(transactions);
			double difference = transaction.amount / mean;

			if (transactions.size() > 10) {
				transactions.remove(0);
			}

			transactions.add(transaction);

			lastTransactions.update(transactions);

			return new Feature("differenceFromMeanInPercent", /*difference*/0, transaction);
		}
	}

	public static double mean(List<Transaction> transactions) {
		double count = (double) transactions.size();
		double total = 0;

		for (Transaction transaction : transactions) {
			total += transaction.amount;
		}

		return total / count;
	}

	public static class CalculateSpeed extends RichMapFunction<Transaction, Feature> {

		private ValueState<Transaction> lastTransactionState;

		@Override
		public void open(Configuration parameters) {
			ValueStateDescriptor<Transaction> valueStateDescriptor = new ValueStateDescriptor<>(
					"last-transaction-per-customer-state",
					Transaction.class
			);

			this.lastTransactionState = getRuntimeContext().getState(valueStateDescriptor);
		}

		@Override
		public Feature map(Transaction transaction) throws Exception {
			Transaction lastTransaction = lastTransactionState.value();

			if (lastTransaction == null) {
				return new Feature("speed", 0.0, transaction);
			}

			double distance = GeoUtils.distance(lastTransaction.location, transaction.location);
			double time = GeoUtils.time(lastTransaction.created, transaction.created);
			double speed = distance / time;

			this.lastTransactionState.update(transaction);

			return new Feature("speed", speed, transaction);
		}
	}

	private static class FraudDetection implements MapFunction<Tuple2<Feature, Feature>, FraudDetectionResult> {

		@Override
		public FraudDetectionResult map(Tuple2<Feature, Feature> features) throws Exception {

			sanityCheck(features);

			boolean fraud = isFraud(features.f0, features.f1);
			return new FraudDetectionResult(fraud, features.f0.transaction);
		}

		private void sanityCheck(Tuple2<Feature, Feature> features) {
			if (!features.f0.transaction.uuid.equals(features.f1.transaction.uuid)) {
				throw new RuntimeException("Features for different transactions were combined");
			}
		}

		private boolean isFraud(Feature first, Feature second) {
			boolean tooFast = (first.value * 3.6 > 200);  // travelling with a speed higher than 200km/h
			boolean tooFarFromMean = (second.value > 10); // more than 10x withdrawel of the usual amount
			return tooFast || tooFarFromMean;
		}
	}

	private static class AddUuid implements MapFunction<Transaction, Transaction> {

		private int count = 0;
		@Override
		public Transaction map(Transaction transaction) throws Exception {
			String uuid = String.valueOf(count++);
			transaction.uuid = uuid;
			return transaction;
		}
	}

	private static class JoinFeatures extends RichCoFlatMapFunction<Feature, Feature, Tuple2<Feature, Feature>> {

		private ValueState<Feature> secondFeature;
		private ValueState<Feature> firstFeature;

		@Override
		public void open(Configuration parameters) throws Exception {
			this.firstFeature = getRuntimeContext().getState(new ValueStateDescriptor<>(
					"first-feature",
					Feature.class
			));

			this.secondFeature = getRuntimeContext().getState(new ValueStateDescriptor<>(
					"second-feature",
					Feature.class
			));
		}

		@Override
		public void flatMap1(
				Feature feature,
				Collector<Tuple2<Feature, Feature>> collector
		) throws Exception {
			if (secondFeature.value() != null) {
				collector.collect(Tuple2.of(feature, secondFeature.value()));
				secondFeature.update(null);
			} else {
				firstFeature.update(feature);
			}
		}

		@Override
		public void flatMap2(
				Feature feature,
				Collector<Tuple2<Feature, Feature>> collector
		) throws Exception {
			if (firstFeature.value() != null) {
				collector.collect(Tuple2.of(firstFeature.value(), feature));
				firstFeature.update(null);
			} else {
				secondFeature.update(feature);
			}
		}
	}
}
