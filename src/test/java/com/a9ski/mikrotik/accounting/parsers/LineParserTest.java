package com.a9ski.mikrotik.accounting.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.a9ski.mikrotik.accounting.exceptions.IllegalRecordException;
import com.a9ski.mikrotik.accounting.model.AccountingRecord;

class LineParserTest {

	private final LineParser lineParser = new LineParser();
	private final AccountingRecord expectedRecord = record("192.168.1.1", "192.168.0.2", 42, 6);

	@Test
	void testParseValidLine() throws IllegalRecordException {
		final AccountingRecord actualRecord = lineParser.parse("192.168.1.1 192.168.0.2 42 6 * *");
		assertEquals(expectedRecord, actualRecord);
	}

	@Test
	void testParseValidLineShort() throws IllegalRecordException {
		final AccountingRecord actualRecord = lineParser.parse("192.168.1.1 192.168.0.2 42 6");
		assertEquals(expectedRecord, actualRecord);
	}

	@Test
	void testParseValidLineWithWhiteSpaces() throws IllegalRecordException {
		final AccountingRecord actualRecord = lineParser.parse("    192.168.1.1				192.168.0.2  	  	  	 42			   		 	 6  	   *		 *");
		assertEquals(expectedRecord, actualRecord);
	}

	@Test
	void testParseInvalidLineWithNotEnoughParameters() throws IllegalRecordException {
		final IllegalRecordException ex = assertThrows(IllegalRecordException.class, () -> { lineParser.parse("192.168.1.1"); } );
		assertEquals("Expected line with 4 parametes but found only 1: '192.168.1.1'", ex.getMessage());
	}

	@Test
	void testParseInvalidLineWithInvalidPackets() throws IllegalRecordException {
		final IllegalRecordException ex = assertThrows(IllegalRecordException.class, () -> { lineParser.parse("192.168.1.1 192.168.0.2 x 6 * *"); } );
		assertEquals("Line with invalid number for field 'byte': '192.168.1.1 192.168.0.2 x 6 * *'", ex.getMessage());
	}

	@Test
	void testParseInvalidLineWithInvalidBytes() throws IllegalRecordException {
		final IllegalRecordException ex = assertThrows(IllegalRecordException.class, () -> { lineParser.parse("192.168.1.1 192.168.0.2 42 x * *"); } );
		assertEquals("Line with invalid number for field 'packet': '192.168.1.1 192.168.0.2 42 x * *'", ex.getMessage());
	}

	private AccountingRecord record(String srcIp, String dstIp, long bytes, long packets) {
		// @formatter:off
		return AccountingRecord.builder()
			.sourceIp(srcIp)
			.destinationIp(dstIp)
			.packetCount(packets)
			.byteCount(bytes)
			.build();
		// @formatter:on
	}

}
