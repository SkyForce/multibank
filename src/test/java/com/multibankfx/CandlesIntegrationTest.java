package com.multibankfx;

import com.multibankfx.model.BidAskEvent;
import com.multibankfx.model.Candle;
import com.multibankfx.service.CandleAggregatorService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class CandlesIntegrationTest {

    static DockerImageName image = DockerImageName.parse("timescale/timescaledb:latest-pg17")
            .asCompatibleSubstituteFor("postgres");
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer(image)
                    .withDatabaseName("multi")
                    .withUsername("postgres")
                    .withPassword("123");
    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CandleAggregatorService aggregator;

    static {
        postgres.start();
    }

    @AfterAll
    static void teardown() {
        postgres.stop();
    }

    private Map<String, Object> row() {
        return jdbc.queryForMap("SELECT * FROM candles_rt LIMIT 1");
    }

    @Test
    void liquibaseRanCorrectly() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM candles_rt", Integer.class);
        assertThat(count).isEqualTo(0); // table exists, empty
    }

    @Test
    void testAggregateCandle5s() {

        aggregator.push(new BidAskEvent("BTC-USD", 1, 2, 120));
        aggregator.push(new BidAskEvent("BTC-USD", 2, 3, 121));
        aggregator.push(new BidAskEvent("BTC-USD", 3, 4, 122));
        aggregator.push(new BidAskEvent("BTC-USD", 4, 5, 123));
        aggregator.push(new BidAskEvent("BTC-USD", 5, 6, 124));
        aggregator.flush();
        List<Candle> range = aggregator.getRange("BTC-USD", "5s", 120, 125);
        assertEquals(1, range.size());
        assertThat(range.getFirst()).isEqualTo(new Candle(120, 1.5, 5.5, 1.5, 5.5, 5));
    }

    @Test
    void testAggregateCandle1s() {

        aggregator.push(new BidAskEvent("BTC-USD", 1, 2, 120));
        aggregator.push(new BidAskEvent("BTC-USD", 1, 2, 121));
        aggregator.push(new BidAskEvent("BTC-USD", 1, 2,122));
        aggregator.push(new BidAskEvent("BTC-USD", 1, 2, 123));
        aggregator.push(new BidAskEvent("BTC-USD", 1, 2, 124));
        aggregator.flush();
        List<Candle> range = aggregator.getRange("BTC-USD", "1s", 120, 125);
        assertEquals(5, range.size());
        int i = 0;
        for(Candle c: range) {
            assertThat(c).isEqualTo(new Candle(120+i, 1.5, 1.5, 1.5, 1.5, 1));
            i++;
        }
    }

}
