package com.ak.binancepricetracker;

import com.ak.binancepricetracker.client.BinanceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "binance.poll-initial-delay=PT1H")
class BinancepricetrackerApplicationTests {

	@MockitoBean
	private BinanceClient binanceClient;

	@BeforeEach
	void configureBinanceClient() {
		when(binanceClient.getAveragePrice("BNBUSDT"))
				.thenReturn(new com.ak.binancepricetracker.client.BinanceAveragePriceResponse(
					5, new BigDecimal("600.00"), 1762000000000L));
	}

	@Test
	@DisplayName("загружает контекст приложения")
	void contextLoads() {
	}

}
