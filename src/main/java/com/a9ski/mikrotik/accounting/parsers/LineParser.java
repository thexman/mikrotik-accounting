package com.a9ski.mikrotik.accounting.parsers;

import com.a9ski.mikrotik.accounting.exceptions.IllegalRecordException;
import com.a9ski.mikrotik.accounting.model.AccountingRecord;

import lombok.extern.log4j.Log4j2;

/**
 * Parser for lines of MikroTik accounting
 * <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">Mirkotik
 * accounting page</a>
 *
 * @author Kiril Arabadzhiyski
 *
 */
@Log4j2
public class LineParser {
	private static final String INVALID_LINE_NOT_A_NUMBER_MSG = "Line with invalid number for field '%s': '%s'";
	private static final String INVALID_LINE_NOT_ENOUGH_PARAMS_MSG = "Expected line with 4 parametes but found only %d: '%s'";

	/**
	 * Parses a single line and creates <tt>AccountingRecord</tt>.
	 * 
	 * @param line the line being parsed
	 * @return <tt>AccountingRecord</tt> representing the line.
	 * @throws IllegalRecordException thrown if there is an error parsing the line.
	 */
	public AccountingRecord parse(final String line) throws IllegalRecordException {
		// 10.0.1.1 10.0.1.2 168 2 * *
		final String[] items = line.strip().split("\\s+");
		if (items.length < 4) {
			throw new IllegalRecordException(String.format(INVALID_LINE_NOT_ENOUGH_PARAMS_MSG, items.length, line));
		}

		// @formatter:off
		return AccountingRecord.builder()
			.sourceIp(items[0])
			.destinationIp(items[1])
			.byteCount(parseLong(line, "byte", items[2]))
			.packetCount(parseLong(line, "packet", items[3]))
			.build();
		// @formatter:on
	}

	private long parseLong(final String line, final String field, final String value) throws IllegalRecordException {
		try {
			return Long.parseLong(value);
		} catch (final NumberFormatException ex) {
			throw new IllegalRecordException(String.format(INVALID_LINE_NOT_A_NUMBER_MSG, field, line));
		}
	}

	/**
	 * Parses a single line and creates <tt>AccountingRecord</tt>. If cannot parse
	 * the line a <tt>null</tt> is returned. The error message is logged via log4j2.
	 *
	 * @param line the line being parsed
	 * @return <tt>AccountingRecord</tt> representing the line or <tt>null</tt> in
	 *         case of a parsing error.
	 */
	public AccountingRecord tryParse(final String line) {
		try {
			return parse(line);
		} catch (final IllegalRecordException ex) {
			log.warn(ex);
			return null;
		}
	}

}
