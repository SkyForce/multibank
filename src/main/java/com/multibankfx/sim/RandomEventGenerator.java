package com.multibankfx.sim;

import com.multibankfx.model.BidAskEvent;
import com.multibankfx.service.CandleAggregatorService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.*;

@Component
@Profile("!test")
public class RandomEventGenerator {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random rnd = new Random();
    private final CandleAggregatorService aggregator;
    private final String[] symbols = {"BTC-USD", "ETH-USD"};

    public RandomEventGenerator(CandleAggregatorService aggregator) {
        this.aggregator = aggregator;
    }

    @PostConstruct
    public void start() {
        // produce 10 events/second (burst) for demo
        scheduler.scheduleAtFixedRate(this::emitBurst, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void emitBurst() {
        long now = Instant.now().getEpochSecond();
        for (int i = 0; i < 3; i++) {
            String symbol = symbols[rnd.nextInt(symbols.length)];
            double mid = symbol.equals("BTC-USD") ? 30000 + rnd.nextGaussian()*200 : 2000 + rnd.nextGaussian()*50;
            double spread = Math.abs(rnd.nextGaussian()) * (symbol.equals("BTC-USD") ? 10 : 1);
            double bid = mid - spread/2.0;
            double ask = mid + spread/2.0;
            BidAskEvent e = new BidAskEvent(symbol, bid, ask, now);
            aggregator.push(e);
        }
    }
}
