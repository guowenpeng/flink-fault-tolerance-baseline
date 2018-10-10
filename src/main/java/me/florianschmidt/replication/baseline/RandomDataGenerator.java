package me.florianschmidt.replication.baseline;

import me.florianschmidt.replication.baseline.model.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomDataGenerator {

	private static int MAX_CUSTOMERS = 1000;
	private static int MAX_CARDS_PER_CUSTOMER = 4;

	private Random random = new Random();

	private int latestCardNumber = 0;
	private Map<Integer, Integer> cardsPerCustomer = new HashMap<>(MAX_CUSTOMERS);

	public int randomCustomerId() {
		return random.nextInt(MAX_CUSTOMERS);
	}

	public int cardNumberForCustomer(int customerId) {
		if (cardsPerCustomer.containsKey(customerId)) {
			return cardsPerCustomer.get(customerId);
		} else {
			cardsPerCustomer.put(customerId, latestCardNumber);
			return latestCardNumber++;
		}
	}

	public Location locationForCustomer(int customerId) {
		// TODO: Fix me and leave a trail of locations behind to make this more realistic
		double latitude = (Math.random() * 180.0) - 90.0;
		double longitude = (Math.random() * 360.0) - 180.0;
		return new Location(latitude, longitude);
	}

	public int timestampForCustomer(int customerId) {
		// TODO: Implement me
		return 0;
	}

	public int amountForCustomer(int customerId) {
		return 0;
	}


}
