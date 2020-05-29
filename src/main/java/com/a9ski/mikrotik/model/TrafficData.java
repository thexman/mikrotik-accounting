package com.a9ski.mikrotik.model;

import lombok.Builder;
import lombok.Data;

/**
 * Summary of the traffic data for given IP address.
 *
 * @author Kiril Arabadzhiyski
 *
 */
@Builder
@Data
public class TrafficData {
	/**
	 * bytes sent from given IP address.
	 */
	private final long bytesSent;
	/**
	 * bytes received from given IP address.
	 */
	private final long bytesReceived;
	/**
	 * packets sent from given IP address.
	 */
	private final long packetsSent;
	/**
	 * packets received from given IP address.
	 */
	private final long packetsReceived;
}
