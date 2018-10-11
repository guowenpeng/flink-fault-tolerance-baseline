package me.florianschmidt.replication.baseline.model;

public class Transaction {

	public int customerId;
	public int cardId;
	public Location location;
	public long utcTimestamp;
	public double amount;
	public long created;

	public String uuid;

	// public final byte[] payload = new byte[100];


	public Transaction() {
	}

	public Transaction(
			int customerId,
			int cardId,
			Location location,
			long utcTimestamp,
			double amount
	) {
		this.customerId = customerId;
		this.cardId = cardId;
		this.location = location;
		this.utcTimestamp = utcTimestamp;
		this.amount = amount;
		this.created = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "Transaction{" +
				"customerId=" + customerId +
				", cardId=" + cardId +
				", location=" + location +
				", utcTimestamp=" + utcTimestamp +
				", amount=" + amount +
				'}';
	}
}
