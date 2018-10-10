package me.florianschmidt.replication.baseline.model;

public class Transaction {

	public final int customerId;
	public final int cardId;
	public final Location location;
	public final long utcTimestamp;
	public final double amount;
	public long created;

	// public final byte[] payload = new byte[100];

	// Make pojo or add serializer
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
