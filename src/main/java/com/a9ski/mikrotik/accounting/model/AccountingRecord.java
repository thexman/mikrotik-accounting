package com.a9ski.mikrotik.accounting.model;

import lombok.Builder;
import lombok.Data;

/**
 * Single line from the MikroTik acounting page.
 *
 * @author Kiril Arabadzhiyski
 *
 */
@Builder
@Data
public class AccountingRecord {
	/**
	 * The source IP address.
	 */
	private final String sourceIp;

	/**
	 * The destination IP address.
	 */
	private final String destinationIp;

	/**
	 * Number of bytes sent from the source IP to the destination IP.
	 */
	private final long byteCount;

	/**
	 * Number of packets sent from the source IP to the destination IP.
	 */
	private final long packetCount;

}
