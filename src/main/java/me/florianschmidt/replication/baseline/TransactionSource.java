package me.florianschmidt.replication.baseline;

import com.google.common.collect.Lists;
import me.florianschmidt.replication.baseline.model.Transaction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.checkpoint.ListCheckpointed;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import java.io.*;
import java.net.Socket;
import java.util.List;

// TODO: Add checkpointing
public class TransactionSource extends RichParallelSourceFunction<Transaction> implements ListCheckpointed<Long> {

	private boolean cancelled = false;

	private transient BufferedReader reader;
	private transient BufferedWriter writer;

	private ObjectMapper mapper;
	private long offset = 0;

	@Override
	public void open(Configuration parameters) throws Exception {
		Socket socket = new Socket(Config.GENERATOR_HOST, Config.GENERATOR_PORT);

		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		this.mapper = new ObjectMapper();

		String jobID = getRuntimeContext().getMetricGroup().getAllVariables().get("<job_id>");

		this.writer.write("START " + jobID + "\n");
		this.writer.write("RESET " +String.valueOf(offset) + "\n");
	}

	@Override
	public void run(SourceContext<Transaction> sourceContext) throws Exception {
		while (!cancelled) {
			synchronized (sourceContext.getCheckpointLock()) {
				Transaction t = generateNewTransaction();
				this.offset = Long.parseLong(t.uuid);
				sourceContext.collect(t);
			}
			Thread.sleep(5);
		}
	}

	@Override
	public void cancel() {
		this.cancelled = true;
	}


	public Transaction parseTransaction(String input) throws IOException {
		return this.mapper.readValue(input, Transaction.class);
	}

	private Transaction generateNewTransaction() throws IOException {

		this.writer.write("GENERATE\n");
		this.writer.flush();

		String input = this.reader.readLine();
		return parseTransaction(input);
	}

	@Override
	public List<Long> snapshotState(long checkpointId, long timestamp) throws Exception {
		return Lists.newArrayList(this.offset);
	}

	@Override
	public void restoreState(List<Long> state) throws Exception {
		this.offset = state.get(0);
	}
}
