package me.florianschmidt.replication.baseline;

import me.florianschmidt.replication.baseline.model.Transaction;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TransactionSourceTest {

	@Test
	@Ignore
	public void testParseTransaction() throws Exception {
		String input = "{\"CustomerID\":85,\"CardID\":41,\"Location\":{\"Latitude\":32.0264274842273,\"Longitude\":-171.89949789807744},\"CreatedAt\":1539518645810310300,\"ID\":3591}\n";
		TransactionSource source = new TransactionSource();
		source.open(null);

		Transaction t = source.parseTransaction(input);

		Assert.assertEquals(85, t.customerId);
		Assert.assertEquals(41, t.cardId);
		Assert.assertEquals(32.0264274842273, t.location.latitude, 0.0);
		Assert.assertEquals(-171.89949789807744, t.location.longitude, 0.0);
		Assert.assertEquals("3591", t.uuid);
	}
}
