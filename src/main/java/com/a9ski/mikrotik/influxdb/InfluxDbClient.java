package com.a9ski.mikrotik.influxdb;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

import com.a9ski.mikrotik.model.TrafficData;

/**
 * Influx DB client.
 *
 * @author Kiril Arabadzhiyski
 *
 */
public class InfluxDbClient implements Closeable {
	private static final String RETENTION_POLICY = "180_days_retention_policy";
	private static final String MEASUREMENT = "IPTrafficData";
	private final InfluxDB influxDB;
	private final String routerIp;
	private final AtomicLong recordsCount = new AtomicLong();

	/**
	 * Creates a new client.
	 *
	 * @param serverUrl    the url to the Influx DB (e.g. http://192.168.1.1:8086)
	 * @param username     user name for the database.
	 * @param password     password for the database.
	 * @param databaseName the database name.
	 * @param routerIp     the router IP address.
	 */
	public InfluxDbClient(final String serverUrl, final String username, final String password, final String databaseName, final String routerIp) {
		this.routerIp = routerIp;
		influxDB = InfluxDBFactory.connect(serverUrl, username, password);
		influxDB.query(new Query(String.format("CREATE DATABASE %s WITH DURATION 180d REPLICATION 1 NAME \"%s\"", databaseName, RETENTION_POLICY)));
		influxDB.setRetentionPolicy(RETENTION_POLICY);
		influxDB.setDatabase(databaseName);
		// influxDB.enableBatch(BatchOptions.DEFAULTS);
	}

	/**
	 * Writes data to the database.
	 *
	 * @param lanIps  the list of IPs belonging to the local area network (LAN)
	 * @param traffic the traffic data. Key is the IP address, Value is summary of
	 *                the traffic for that IP.
	 */
	public void write(final Set<String> lanIps, final Map<String, TrafficData> traffic) {
		final long now = System.currentTimeMillis();

		// Write points to InfluxDB.
		//@formatter:off
		traffic.keySet().stream()
			.map(ip -> createPoint(now, ip, lanIps.contains(ip), traffic.get(ip)))
			.forEach(this::write);
		//@formatter:on
	}

	/**
	 * Writes a point to the database.
	 *
	 * @param point the point to be written.
	 */
	private void write(Point point) {
		influxDB.write(point);
		recordsCount.incrementAndGet();
	}

	/**
	 * Creates a point for given timestamp and ip.
	 *
	 * @param timestamp   the timestamp of the point.
	 * @param ip          the IP addres
	 * @param isLanIp     boolean flag indicating that the IP belongs to the local
	 *                    area network (LAN)
	 * @param trafficData summary of the traffic for the given IP.
	 * @return
	 */
	private Point createPoint(long timestamp, String ip, boolean isLanIp, TrafficData trafficData) {
		//@formatter:off
		return Point.measurement(MEASUREMENT)
		    .time(timestamp, TimeUnit.MILLISECONDS)
		    .tag("ip", ip)
		    .tag("type", isLanIp ? "LAN" : "WAN")
		    .tag("routerIp", routerIp)
		    .addField("isWan", isLanIp ? 0 : 1)
		    .addField("bytesSent", trafficData.getBytesSent())
		    .addField("bytesReceived", trafficData.getBytesReceived())
		    .addField("packetsSent", trafficData.getPacketsSent())
		    .addField("packetsReceived", trafficData.getPacketsReceived())
		    .build();
	    //@formatter:off
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		influxDB.close();
	}

	/**
	 * Gets the number of records written with that client.
	 * @return the number of records written with that client.
	 */
	public long getRecordsCount() {
		return recordsCount.get();
	}
}
