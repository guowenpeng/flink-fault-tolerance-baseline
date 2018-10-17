package me.florianschmidt.replication.baseline;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import me.florianschmidt.replication.baseline.model.Transaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

class TimeMeasureSink extends RichSinkFunction<Transaction> {

	private transient BufferedWriter writer;
	private transient BufferedReader reader;
	private int lastAcknowledged;

	@Override
	public void open(Configuration parameters) throws Exception {
		Socket socket = new Socket(Config.GENERATOR_HOST, Config.GENERATOR_PORT);
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


		String jobID = getRuntimeContext().getMetricGroup().getAllVariables().get("<job_id>");

		this.writer.write("START "+jobID+"\n");
		this.writer.write("GET_LAST_ACKNOWLEDGED");
		this.writer.write("\n");
		this.writer.flush();

		this.lastAcknowledged = Integer.parseInt(this.reader.readLine());
	}

	@Override
	public void invoke(Transaction transaction, Context context) throws Exception {

		if (Integer.parseInt(transaction.uuid) > this.lastAcknowledged) {
			this.writer.write("ACKNOWLEDGE " + transaction.uuid + "\n");
			this.writer.flush();
		} else {
//			 do nothing, we are still catching up
		}
	}
}
