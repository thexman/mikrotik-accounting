package com.a9ski.mikrotik;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.a9ski.mikrotik.accounting.AccountingClient;
import com.a9ski.mikrotik.accounting.model.AccountingRecord;
import com.a9ski.mikrotik.influxdb.InfluxDbClient;
import com.a9ski.mikrotik.model.TrafficData;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Service responsible for reading traffic data from router and write it to database.
 *
 * @author Kiril Arabadzhiyski
 *
 */
@Log4j2
public class TrafficService implements Closeable {
	/**
	 * Default value for max number of retries for DB operation.
	 */
	private static final int MAX_RETRIES = 3;
	/**
	 * Number of milliseconds to sleep between executions of the read/write routine.
	 */
	private static final long SLEEP_TIME = 10000;
	/**
	 * Default value used for bytes/packets.
	 */
	private static final Long DEFAULT_VALUE = Long.valueOf(0L);

	private final AccountingClient accountingClient;
	private final InfluxDbClient dbClient;
	private final List<IPAddressString> routerSubnets;
	private final AtomicLong iterations = new AtomicLong();
	private final long sleepTime;
	private final int maxRetries;

	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	/**
	 * Creates a new object.
	 * @param routerIp the router IP address.
	 * @param routerSubNets router subnets (e.g. 192.168.0.0/24)
	 * @param dbUrl the URL for Influx DB (e.g. http://192.168.1.1:8086)
	 * @param dbUserName the database user name
	 * @param dbPassword the database password
	 * @param dbName the database name
	 * @throws AddressStringException thrown if the subnets are not valid.
	 */
	public TrafficService(@NonNull final String routerIp, @NonNull final List<String> routerSubNets, @NonNull final String dbUrl, @NonNull final String dbUserName, @NonNull final String dbPassword, @NonNull final String dbName) throws AddressStringException {
		this(new AccountingClient(routerIp),
				routerSubNets.stream().map(net -> new IPAddressString(net)).collect(Collectors.toList()),
				new InfluxDbClient(dbUrl, dbUserName, dbPassword, dbName, routerIp),
				SLEEP_TIME,
				MAX_RETRIES);
	}

	/**
	 * Creates a new object
	 * @param accountingClient the MikroTik accounting client.
	 * @param routerSubnets the router subnets.
	 * @param dbClient the InfuxDB client.
	 * @param sleepTime time to sleep between executions of the routine that reads traffic data from the router and writes it to the database.
	 * @param maxRetries max number of retries for writing data to database.
	 * @throws AddressStringException thrown if the subnets are not valid.
	 */
	public TrafficService(@NonNull final AccountingClient accountingClient, @NonNull final Collection<IPAddressString> routerSubnets, @NonNull final InfluxDbClient dbClient, long sleepTime, int maxRetries) throws AddressStringException {
		validateSubnets(routerSubnets);
		this.accountingClient = accountingClient;
		this.routerSubnets = new ArrayList<>(routerSubnets);
		this.dbClient = dbClient;
		this.sleepTime = sleepTime;
		this.maxRetries = maxRetries;
		if (routerSubnets.isEmpty()) {
			throw new IllegalArgumentException("Invalid sub nets value. Expected at least one LAN subnet");
		}
		if (sleepTime < 1) {
			throw new IllegalArgumentException("Invalid sleep time. Expected positive value");
		}
		if (maxRetries < 1) {
			throw new IllegalArgumentException("Invalid max retries value. Expected positive value");
		}
		start();
	}

	/**
	 * Validate LAN subnets.
	 * @param ips LAN subnets
	 * @throws AddressStringException thrown if there is invalid subnet.
	 */
	protected void validateSubnets(@NonNull Collection<IPAddressString> ips) throws AddressStringException {
		for(IPAddressString ip : ips) {
			ip.validate();
		}
	}

	/**
	 * Starts the routine that reads from the router and writes into the database.
	 */
	protected void start() {
		executorService.scheduleAtFixedRate(() -> run(), 0L, sleepTime, TimeUnit.MILLISECONDS);
	}

	/**
	 * Stops the routine that reads from the router and writes into the database.
	 * @throws InterruptedException thrown if interrupted while waiting to stop the service.
	 */
	protected void stop() throws InterruptedException {
		executorService.shutdown();
		executorService.awaitTermination(3 * sleepTime, TimeUnit.MILLISECONDS);
	}

	/**
	 * The routine that reads from the router and writes into the database.
	 */
	protected void run() {
		try {
			final List<AccountingRecord> records = accountingClient.loadRecords();

			final Map<String, Long> bytesSent = new TreeMap<>();
			final Map<String, Long> bytesReceived = new TreeMap<>();
			final Map<String, Long> packetsSent = new TreeMap<>();
			final Map<String, Long> packetsReceived = new TreeMap<>();
			records.forEach(r -> {
				bytesSent.compute(r.getSourceIp(), add(r.getByteCount()));
				packetsSent.compute(r.getSourceIp(), add(r.getPacketCount()));
				bytesReceived.compute(r.getDestinationIp(), add(r.getByteCount()));
				packetsReceived.compute(r.getDestinationIp(), add(r.getPacketCount()));
			});

			final Map<String, TrafficData> traffic = new TreeMap<>();
			packetsSent.keySet().forEach(ip -> traffic.put(ip, createTrafficData(ip, bytesSent, bytesReceived, packetsSent, packetsReceived)));

			packetsReceived.keySet().stream()
				.filter(ip -> !packetsSent.containsKey(ip))
				.forEach(ip -> traffic.put(ip, createTrafficData(ip, bytesSent, bytesReceived, packetsSent, packetsReceived)));

			// @formatter:off
			final Set<String> lanIps = traffic.keySet().stream()
					.filter(this::isLanIp)
					.collect(Collectors.toCollection(TreeSet::new));
			// @formatter:on

			writeToDatabase(lanIps, traffic);
			iterations.incrementAndGet();
		} catch (InterruptedException ex) {
			log.error(ex);
			Thread.interrupted();
		} catch (Exception ex) {
			log.error(ex);
		}
	}

	/**
	 * Writes traffica data to database.
	 * @param lanIps list of IPs belonging to local area network (LAN)
	 * @param traffic the traffic for each IP.
	 */
	private void writeToDatabase(final Set<String> lanIps, final Map<String, TrafficData> traffic) {
		final IntervalFunction intervalFn = IntervalFunction.ofExponentialRandomBackoff();

		final RetryConfig retryConfig = RetryConfig.custom()
				.maxAttempts(maxRetries)
				.intervalFunction(intervalFn)
				.build();

		final Retry retry = Retry.of("dbClient.write", retryConfig);

		final Runnable runnable = Retry.decorateRunnable(retry, () -> dbClient.write(lanIps, traffic));
		runnable.run();
	}

	/**
	 * Check if the IP belongs to the local area network (LAN).
	 * @param ip the IP to be checked
	 * @return if the IP is belonging to the LAN.
	 */
	private boolean isLanIp(String ip) {
		final IPAddressString ipAddr = new IPAddressString(ip);
		// @formatter:off
		return routerSubnets.stream()
				.filter(subnet -> subnet.contains(ipAddr))
				.findAny()
				.isPresent();
		// @formatter:on
	}

	/**
	 * Creates a traffic data based on the maps with bytes and packets sent/received for each IP address.
	 * @param ip the IP address
	 * @param bytesSent map with bytes sent by each IP address.
	 * @param bytesReceived map with bytes received by each IP address.
	 * @param packetsSent map with packets sent by each IP address.
	 * @param packetsReceived map with packets received by each IP address.
	 * @return new traffic data object.
	 */
	private TrafficData createTrafficData(String ip, final Map<String, Long> bytesSent, final Map<String, Long> bytesReceived, final Map<String, Long> packetsSent,
			final Map<String, Long> packetsReceived) {
		// @formatter:off
		return TrafficData.builder()
				.bytesSent(bytesSent.getOrDefault(ip, DEFAULT_VALUE))
				.packetsSent(packetsSent.getOrDefault(ip, DEFAULT_VALUE))
				.bytesReceived(bytesReceived.getOrDefault(ip, DEFAULT_VALUE))
				.packetsReceived(packetsReceived.getOrDefault(ip, DEFAULT_VALUE))
				.build();
		// @formatter:on
	}

	/**
	 * Creates a BiFunction that adds <tt>value</tt> to a map value for IP address.
	 * @param value the value to be added
	 * @return the BiFunction to be used with map.compute(...)
	 */
	private BiFunction<String, Long, Long> add(long value) {
		return (k, oldVal) -> oldVal != null ? oldVal.longValue() : 0 + value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		try {
			stop();
		} catch (InterruptedException ex) {
			Thread.interrupted();
		} finally {
			dbClient.close();
		}
	}

	/**
	 * Gets the number of record written to the database with this service.
	 * @return  number of record written to the database with this service.
	 */
	public long getRecordsCount() {
		return dbClient.getRecordsCount();
	}

	/**
	 * Number of iteration loops of the read/write routine.
	 * @return Number of iterations of the read/write routine.
	 */
	public long getIterationsCount() {
		return iterations.get();
	}
}
