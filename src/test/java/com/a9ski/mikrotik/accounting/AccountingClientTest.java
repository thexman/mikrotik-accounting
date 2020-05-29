package com.a9ski.mikrotik.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.a9ski.mikrotik.accounting.exceptions.AccountingException;
import com.a9ski.mikrotik.accounting.model.AccountingRecord;

@ExtendWith(MockitoExtension.class)
class AccountingClientTest {

	private HttpRequest httpRequest;
	@Mock
	private HttpClient httpClient;
	@Mock
	private HttpResponse<String> httpResponse;

	private final Map<HttpRequest, HttpResponse<String>> responses = new HashMap<>();

	@BeforeEach
	void setUp() throws Exception {
		httpRequest = HttpRequest.newBuilder().uri(URI.create("http://192.168.1.1")).GET().build();

		Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.any())).thenAnswer(invocation -> responses.get(invocation.getArgument(0)));
	}

	@Test
	void testLoadWithSuccess() throws AccountingException, InterruptedException, IOException {
		AccountingClient client = new AccountingClient("192.168.1.1") {
			protected HttpClient createHttpClient() {
				return httpClient;
			}

			@Override
			protected HttpRequest createHttpRequest() {
				return httpRequest;
			}
		};

		Mockito.doReturn(200).when(httpResponse).statusCode();
		Mockito.doReturn("192.168.1.1 192.168.0.2 42 6 * *\r\n192.168.1.2 192.168.0.3 42 6 * *").when(httpResponse).body();

		responses.put(httpRequest, httpResponse);

		final List<AccountingRecord> expected = Arrays.asList(record("192.168.1.1", "192.168.0.2"), record("192.168.1.2", "192.168.0.3"));
		assertEquals(expected, client.loadRecords());
	}

	@Test
	void testLoadWithStatusCode500() throws AccountingException, InterruptedException, IOException {
		AccountingClient client = new AccountingClient("192.168.1.1") {
			protected HttpClient createHttpClient() {
				return httpClient;
			}

			@Override
			protected HttpRequest createHttpRequest() {
				return httpRequest;
			}
		};

		Mockito.doReturn(500).when(httpResponse).statusCode();

		responses.put(httpRequest, httpResponse);

		final AccountingException ex = assertThrows(AccountingException.class, () -> client.loadRecords());
		assertEquals("Error listing accounting records. Received http status code 500", ex.getMessage());
	}

	@Test
	void testLoadWithEmptyBodyContent() throws AccountingException, InterruptedException, IOException {
		AccountingClient client = new AccountingClient("192.168.1.1") {
			protected HttpClient createHttpClient() {
				return httpClient;
			}

			@Override
			protected HttpRequest createHttpRequest() {
				return httpRequest;
			}
		};

		Mockito.doReturn(200).when(httpResponse).statusCode();
		Mockito.doReturn("Unexpected content").when(httpResponse).body();

		responses.put(httpRequest, httpResponse);

		assertEquals(Collections.emptyList(), client.loadRecords());
	}

	@Test
	void testLoadWithBodyContainingInvalidLines() throws AccountingException, InterruptedException, IOException {
		AccountingClient client = new AccountingClient("192.168.1.1") {
			protected HttpClient createHttpClient() {
				return httpClient;
			}

			@Override
			protected HttpRequest createHttpRequest() {
				return httpRequest;
			}
		};

		Mockito.doReturn(200).when(httpResponse).statusCode();
		Mockito.doReturn("192.168.1.1 192.168.0.2 42 6 * *\r\nUnexpected content\r\n192.168.1.2 192.168.0.3 42 6 * *").when(httpResponse).body();

		responses.put(httpRequest, httpResponse);

		responses.put(httpRequest, httpResponse);

		final List<AccountingRecord> expected = Arrays.asList(record("192.168.1.1", "192.168.0.2"), record("192.168.1.2", "192.168.0.3"));
		assertEquals(expected, client.loadRecords());
	}

	private AccountingRecord record(String srcIp, String dstIp) {
		// @formatter:off
		return AccountingRecord.builder()
			.sourceIp(srcIp)
			.destinationIp(dstIp)
			.packetCount(6)
			.byteCount(42)
			.build();
		// @formatter:on
	}

}
