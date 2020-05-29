package com.a9ski.mikrotik;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.a9ski.mikrotik.accounting.exceptions.AccountingException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import inet.ipaddr.AddressStringException;
import lombok.extern.log4j.Log4j2;

/**
 * Main entry point of the application.
 *
 */
@Log4j2
public class Application {
	@Parameter(names = { "--console", "-c" }, description = "Console mode")
	private boolean consoleMode;

	@Parameter(names = { "--router-ip", "-r" }, required = true, description = "Router IP addres")
	private String routerIp;

	@Parameter(names = { "--db-url", "-d" }, required = true, description = "Database URL (e.g. http://192.168.1.1:8086)")
	private String dbUrl;

	@Parameter(names = { "--db-user", "-u" }, required = true, description = "Database user")
	private String dbUser;

	@Parameter(names = { "--db-password", "-p" }, required = true, description = "Database password")
	private String dbPassword;

	@Parameter(names = { "--db-name", "-db" }, required = true, description = "Database name")
	private String dbName;

	@Parameter(names = { "--subnet", "-n" }, required = true, variableArity = true, description = "LAN subnets (e.g. 192.168.1.0/24)")
	public List<String> subnets = new ArrayList<>();

	@Parameter(names = { "--help", "-h" }, help = true)
	private boolean help;

	private long lastRecordsCount = 0;

	/**
	 * The main method of the application.
	 *
	 * @param args command line arguments
	 * @throws AccountingException    thrown if error occurs during communcation
	 *                                with MikroTik.
	 * @throws IOException            throw if an input/output error occures
	 * @throws InterruptedException   throw if program is interrupted.
	 * @throws AddressStringException thrown if there is invalid subnet.
	 */
	public static void main(String[] args) throws AccountingException, IOException, InterruptedException, AddressStringException {
		final Application app = new Application();
		//@formatter:off
        final JCommander c = JCommander.newBuilder()
            .addObject(app)
            .build();
        //@formatter:on
		c.setProgramName("mikrotik-accounting");
		try {
			c.parse(args);
			app.run();
		} catch (final ParameterException ex) {
			System.err.println(ex.getMessage());
			c.usage();

		}

	}

	/**
	 * Main routine.
	 *
	 * @throws InterruptedException   throw when the app is interrupted.
	 * @throws AddressStringException thrown if there is invalid subnet.
	 */
	private void run() throws InterruptedException, AddressStringException {
		final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		try (TrafficService service = new TrafficService(routerIp, subnets, dbUrl, dbUser, dbPassword, dbName)) {
			executor.scheduleAtFixedRate(() -> printInfo(service), 10, 30, TimeUnit.SECONDS);
			if (consoleMode) {
				try (Scanner scanner = new Scanner(System.in)) {
					System.out.println("System ready. Press enter to exit");
					scanner.nextLine();
				}
				System.out.println("Exiting...");
			} else {
				while (true) {
					Thread.sleep(1000);
				}
			}
		} finally {
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}
	}

	/**
	 * Prints information about current statistics.
	 *
	 * @param service
	 */
	public void printInfo(TrafficService service) {
		final long records = service.getRecordsCount();
		final long iterations = service.getIterationsCount();
		log.info(String.format("[%s] Iteration %d: %d records (total %d, avg: %f) ", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), iterations, records - lastRecordsCount, records,
				records / (double) iterations));
		lastRecordsCount = records;
	}
}
