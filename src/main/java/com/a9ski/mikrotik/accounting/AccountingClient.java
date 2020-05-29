package com.a9ski.mikrotik.accounting;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.a9ski.mikrotik.accounting.exceptions.AccountingException;
import com.a9ski.mikrotik.accounting.model.AccountingRecord;
import com.a9ski.mikrotik.accounting.parsers.LineParser;

import lombok.NonNull;

/**
 * @author Kiril Arabadzhiyski
 *
 */
public class AccountingClient {
	private static final String INVALID_ERROR_CODE_MSG = "Error listing accounting records. Received http status code %d";

	private final URI uri;

	/**
	 * Creates new MikroTik accounting client. It retrieves information from
	 * <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">Mirkotik
	 * accounting page</a>
	 *
	 * @param routerHost the host name or IP address the Mikrotik router. The
	 *                   accounting URI is constructed as
	 *                   <tt>http://routerHost/accounting/ip.cgi</tt>
	 * @see <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">MicroTik
	 *      manual: IP/Accounting</a>
	 */
	public AccountingClient(@NonNull final String routerHost) {
		this(URI.create(String.format("http://%s/accounting/ip.cgi", routerHost)));
	}

	/**
	 * Creates new MikroTik accounting client. It retrieves information from
	 * <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">Mirkotik
	 * accounting page</a>
	 *
	 * @param routerUri the URI of the Mikrotik accounting page. Usually this is
	 *                  <tt>http://routerIP/accounting/ip.cgi</tt>
	 * @see <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">MicroTik
	 *      manual: IP/Accounting</a>
	 */
	public AccountingClient(final URI routerUri) {
		this.uri = routerUri;
	}

	/**
	 * Creates a new HTTP client.
	 *
	 * @return new HTTP client.
	 */
	protected HttpClient createHttpClient() {
		// @formatter:off
		final HttpClient client = HttpClient.newBuilder()
				.version(Version.HTTP_1_1)
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(3))
				.build();
		return client;
		// @formatter:on
	}

	/**
	 * Creates a new HTTP request for the <tt>uri</tt>
	 *
	 * @return new HTTP request.
	 */
	protected HttpRequest createHttpRequest() {
		// @formatter:off
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(this.uri)
				.header("Content-Type", "text/plain")
				.GET()
				.build();
		return request;
		// @formatter:on
	}

	/**
	 * Creates a new line parser for the response body.
	 *
	 * @return new line parser for the response body.
	 */
	protected LineParser createLineParser() {
		return new LineParser();
	}

	/**
	 * Retrieves information from
	 * <a href="https://wiki.mikrotik.com/wiki/Manual:IP/Accounting">Mirkotik
	 * accounting page</a> and returns a list of accounting records.
	 *
	 * @return list of accounting records
	 * @throws AccountingException  thrown if accounting page cannot be parsed.
	 * @throws IOException          thrown if a communication error occurs.
	 * @throws InterruptedException thrown if the current thread is interrupted.
	 */
	public List<AccountingRecord> loadRecords() throws AccountingException, InterruptedException, IOException {
		//@formatter:off
		final HttpRequest request = createHttpRequest();
		final HttpClient client = createHttpClient();
		final HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		//@formatter:on

		if (response.statusCode() != 200) {
			throw new AccountingException(String.format(INVALID_ERROR_CODE_MSG, response.statusCode()));
		}

		return parseBody(response.body());
	}

	/**
	 * Parses the response body and returns a list of accounting records.
	 *
	 * @param body the response body.
	 * @return list of accounting records.
	 */
	protected List<AccountingRecord> parseBody(final String body) {
		final LineParser lineParser = createLineParser();
		// @formatter:off
		return body.lines()
				.filter(s -> !s.isBlank())
				.map(lineParser::tryParse)
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableList());
		// @formatter:on
	}

}
