package com.multibankfx.rest;

import com.multibankfx.model.Candle;
import com.multibankfx.service.CandleAggregatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final CandleAggregatorService aggregator;

    public HistoryController(CandleAggregatorService aggregator) {
        this.aggregator = aggregator;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(name = "symbol") String symbol,
            @RequestParam(name = "interval") String interval,
            @RequestParam(name = "from") long from,
            @RequestParam(name = "to") long to
    ) {
        var candles = aggregator.getRange(symbol, interval, from, to);

        List<Long> ts = new ArrayList<>();
        List<Double> o = new ArrayList<>();
        List<Double> h = new ArrayList<>();
        List<Double> l = new ArrayList<>();
        List<Double> c = new ArrayList<>();
        List<Long> v = new ArrayList<>();

        for (Candle candle : candles) {
            ts.add(candle.time());
            o.add(candle.open());
            h.add(candle.high());
            l.add(candle.low());
            c.add(candle.close());
            v.add(candle.volume());
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("s", "ok");
        res.put("t", ts);
        res.put("o", o);
        res.put("h", h);
        res.put("l", l);
        res.put("c", c);
        res.put("v", v);

        return ResponseEntity.ok(res);
    }

    private long parseIntervalToSeconds(String interval) {
        interval = interval.trim().toLowerCase();
        if (interval.endsWith("s")) {
            return Long.parseLong(interval.substring(0, interval.length()-1));
        } else if (interval.endsWith("m")) {
            return Long.parseLong(interval.substring(0, interval.length()-1)) * 60;
        } else if (interval.endsWith("h")) {
            return Long.parseLong(interval.substring(0, interval.length()-1)) * 3600;
        } else {
            // default assume seconds numeric
            return Long.parseLong(interval);
        }
    }
}

