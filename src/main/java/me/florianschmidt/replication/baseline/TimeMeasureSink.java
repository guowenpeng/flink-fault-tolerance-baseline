package me.florianschmidt.replication.baseline;

import com.codahale.metrics.SlidingWindowReservoir;
import me.florianschmidt.replication.baseline.model.FraudDetectionResult;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.dropwizard.metrics.DropwizardHistogramWrapper;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

class TimeMeasureSink extends RichSinkFunction<FraudDetectionResult> {

	private Histogram histogram;

	@Override
	public void open(Configuration parameters) throws Exception {
		com.codahale.metrics.Histogram dropwizardHistogram =
				new com.codahale.metrics.Histogram(new SlidingWindowReservoir(500));

		this.histogram = getRuntimeContext()
				.getMetricGroup()
				.histogram("myHistogram", new DropwizardHistogramWrapper(dropwizardHistogram));
	}

	@Override
	public void invoke(FraudDetectionResult value, Context context) throws Exception {
		long latency = System.currentTimeMillis() - value.transaction.created;
		histogram.update(latency);

		if (value.isFraud) {
			System.out.println("Fraud detected: " + value.transaction);
		} else {
			System.out.println("No fraud: " + value.transaction);
		}
	}
}
