package me.florianschmidt.replication.baseline;

import me.florianschmidt.replication.baseline.model.Transaction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

// TODO: Add checkpointing
public class TransactionSource extends RichParallelSourceFunction<Transaction> {

	private boolean cancelled = false;
	private transient RandomDataGenerator randomDataGenerator;

	@Override
	public void open(Configuration parameters) throws Exception {
		randomDataGenerator = new RandomDataGenerator();
	}

	@Override
	public void run(SourceContext<Transaction> sourceContext) throws Exception {
		while (!cancelled) {
			synchronized (sourceContext.getCheckpointLock()) {
				sourceContext.collect(generateNewTransaction());
			}
			Thread.sleep(5);
		}
	}

	@Override
	public void cancel() {
		this.cancelled = true;
	}

	private Transaction generateNewTransaction() {

		int customerId = randomDataGenerator.randomCustomerId();

		return new Transaction(
				customerId,
				randomDataGenerator.cardNumberForCustomer(customerId),
				randomDataGenerator.locationForCustomer(customerId),
				randomDataGenerator.timestampForCustomer(customerId),
				randomDataGenerator.amountForCustomer(customerId)
		);
	}
}
