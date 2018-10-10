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

import com.google.common.math.Quantiles;
import com.google.common.math.Stats;
import me.florianschmidt.replication.baseline.model.Transaction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.metrics.HistogramStatistics;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.shaded.guava18.com.google.common.primitives.Longs;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.util.LinkedList;
import java.util.List;


public class StreamingJob {

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		// env.enableCheckpointing(Time.seconds(30).toMilliseconds());

		env.setParallelism(1);
		boolean useIncrementalCheckpointing = true;
		StateBackend fileBackend = new FsStateBackend("file:///tmp/flink-docker/");
//		 RocksDBStateBackend backend = new RocksDBStateBackend((AbstractStateBackend)fileBackend, true);

		// disable latency tracking
		env.getConfig().setLatencyTrackingInterval(-1);

		// env.setStateBackend(backend);

		env
				.addSource(new TransactionSource())
				.name("Transaction Generator")
				.uid("TransactionGenerator")
				.keyBy(transaction -> transaction.customerId)

				.map(new FraudDetection())
				.uid("FraudDetectionOperator")
				.name("Fraud Detection")

				.addSink(new TimeMeasure())
				.uid("MeasuringLatencySink")
				.name("Measuring Latency Sink");

		env.execute("Fraud detection example");
	}

	private static class TimeMeasure extends RichSinkFunction<FraudDetectionResult> {

		private Histogram histogram;

		@Override
		public void open(Configuration parameters) throws Exception {
			this.histogram = this.getRuntimeContext()
					.getMetricGroup()
					.histogram("customLatencyHistogram", new Histogram() {

						private List<Long> values = new LinkedList<>();

						@Override
						public void update(long l) {
							this.values.add(l);
						}

						@Override
						public long getCount() {
							return this.values.size();
						}

						@Override
						public HistogramStatistics getStatistics() {
							return new MyHistogramStatistics(values);
						}
					});
		}

		@Override
		public void invoke(FraudDetectionResult value, Context context) throws Exception {
			long latency = System.currentTimeMillis() - value.transaction.created;
			histogram.update(latency);
		}

		private static class MyHistogramStatistics extends HistogramStatistics {

			private final List<Long> values;

			public MyHistogramStatistics(List<Long> values) {
				this.values = values;
			}

			@Override
			public double getQuantile(double quantile) {
				if (values.size() == 0) {
					return 0;
				}
				return Quantiles.percentiles().index((int) quantile).compute(values);
			}

			@Override
			public long[] getValues() {
				return Longs.toArray(values);
			}

			@Override
			public int size() {
				return values.size();
			}

			@Override
			public double getMean() {
				if (values.size() == 0) {
					return 0;
				}
				return Stats.meanOf(values);
			}

			@Override
			public double getStdDev() {
				if (values.size() == 0) {
					return 0;
				}
				return Stats.of(values).sampleStandardDeviation();
			}

			@Override
			public long getMax() {
				if (values.size() == 0) {
					return 0;
				}
				return (long) Stats.of(values).max();
			}

			@Override
			public long getMin() {
				if (values.size() == 0) {
					return 0;
				}
				return (long) Stats.of(values).min();
			}
		}
	}

	private static class FraudDetection extends RichMapFunction<Transaction, FraudDetectionResult> {

		@Override
		public void open(Configuration parameters) throws Exception {

		}

		@Override
		public FraudDetectionResult map(Transaction transaction) throws Exception {

			boolean fraud = isFraud(null, transaction);
			// previousTransactions.add(transaction);
			return new FraudDetectionResult(fraud, transaction);
		}

		public boolean isFraud(
				Transaction previousTransaction,
				Transaction currentTransaction
		) throws InterruptedException {
			Thread.sleep(2);
			return false;
		}
	}

	private static class FraudDetectionResult {
		public final boolean isFraud;
		public final Transaction transaction;

		public FraudDetectionResult(boolean isFraud, Transaction transaction) {
			this.isFraud = isFraud;
			this.transaction = transaction;
		}

		@Override
		public String toString() {
			return "FraudDetectionResult{" +
					"isFraud=" + isFraud +
					", transaction=" + transaction +
					'}';
		}
	}
}
