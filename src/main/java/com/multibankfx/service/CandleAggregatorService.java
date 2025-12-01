package com.multibankfx.service;

import com.multibankfx.model.BidAskEvent;
import com.multibankfx.model.Candle;
import com.multibankfx.repository.TimescaleRTCandleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class CandleAggregatorService {
    private final TimescaleBatchUpsertWriter writer;
    private final List<BidAskEvent> buffer = new ArrayList<>();
    private final Object lock = new Object();
    private final TimescaleRTCandleRepository timescaleRTCandleRepository;
    @Autowired
    private JdbcTemplate jdbc;

    public CandleAggregatorService(TimescaleBatchUpsertWriter writer, TimescaleRTCandleRepository timescaleRTCandleRepository) {
        this.writer = writer;
        this.timescaleRTCandleRepository = timescaleRTCandleRepository;

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::flush, 1, 1, TimeUnit.SECONDS);
    }

    public void push(BidAskEvent row) {
        synchronized (lock) {
            buffer.add(row);
        }
    }

    public void flush() {
        List<BidAskEvent> copy;
        synchronized (lock) {
            if (buffer.isEmpty()) return;
            copy = new ArrayList<>(buffer);
            buffer.clear();
        }
        try {
            writer.saveBatch(copy);
            timescaleRTCandleRepository.saveBatch(copy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Candle> getRange(
            String symbol, String interval, long from, long to) {

        return jdbc.query("""
                        SELECT time, open, high, low, close, volume
                        FROM candles_rt
                        WHERE symbol = ?
                            AND interval = ?
                          AND time BETWEEN ? AND ?
                        ORDER BY time ASC
                        """,
                ps -> {
                    ps.setString(1, symbol);
                    ps.setString(2, interval);
                    ps.setLong(3, from);
                    ps.setLong(4, to);
                },
                (rs, rowNum) -> new Candle(
                        rs.getLong("time"),
                        rs.getDouble("open"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("close"),
                        rs.getLong("volume")
                )
        );
    }

    public List<Candle> getRangeFromView(String symbol, String interval, long from, long to) {

        return jdbc.query(String.format("""
                        SELECT bucket, open, high, low, close, volume
                        FROM cagg_%s
                        WHERE symbol = ?
                          AND bucket BETWEEN ? AND ?
                        ORDER BY bucket ASC
                        """, interval),
                ps -> {
                    ps.setString(1, symbol);
                    ps.setTimestamp(2, Timestamp.from(Instant.ofEpochSecond(from)));
                    ps.setTimestamp(3, Timestamp.from(Instant.ofEpochSecond(to)));
                },
                (rs, rowNum) -> new Candle(
                        rs.getTimestamp("bucket").toInstant().getEpochSecond(),
                        rs.getDouble("open"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("close"),
                        rs.getLong("volume")
                )
        );
    }
}