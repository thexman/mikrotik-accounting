package com.a9ski.mikrotik.accounting;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.a9ski.mikrotik.accounting.exceptions.AccountingException;

class AccountingClientIntegrationTest {

	private String routerIp;

	@BeforeEach
	void setUp() throws Exception {
		this.routerIp = System.getProperty("routerIp", System.getenv("ROUTER_IP"));
	}

	@Test
	@EnabledIfSystemProperty(named = "routerIp", matches = ".+" )
	void testLoadFromRouter_SystemProp() throws AccountingException, InterruptedException, IOException {
		testLoadFromRouter();
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "ROUTER_IP", matches = ".+" )
	void testLoadFromRouter_EnvVar() throws AccountingException, InterruptedException, IOException {
		testLoadFromRouter();
	}

	private void testLoadFromRouter() throws AccountingException, InterruptedException, IOException {
		final AccountingClient client = new AccountingClient(routerIp);
		client.loadRecords().stream().forEach(System.out::println);
	}

}
