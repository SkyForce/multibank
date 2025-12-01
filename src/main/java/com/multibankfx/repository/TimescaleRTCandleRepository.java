package com.multibankfx.repository;

import com.multibankfx.model.BidAskEvent;
import com.multibankfx.model.Candle;
import com.multibankfx.model.Interval;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


@Repository
public class TimescaleRTCandleRepository {


    private final DataSource dataSource;
    private final List<Interval> intervals = List.of(new Interval(1, "1s"), new Interval(5, "5s"),
            new Interval(60, "1m"), new Interval(300, "5m"), new Interval(900, "15m"));



    public TimescaleRTCandleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public void saveBatch(List<BidAskEvent> candles) throws Exception {
        String sql = """
                INSERT INTO candles_rt(symbol, interval, time, open, high, low, close, volume)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (symbol, interval, time) DO UPDATE SET
                            high = GREATEST(candles_rt.high, EXCLUDED.high),
                            low  = LEAST(candles_rt.low, EXCLUDED.low),
                            close = EXCLUDED.close,
                            volume = candles_rt.volume + EXCLUDED.volume
                """;


        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {


            conn.setAutoCommit(false);


            for (BidAskEvent c : candles) {
                double price = (c.bid() + c.ask()) / 2.0;
                for(Interval interval: intervals) {
                    long bucket = c.timestamp() / interval.seconds() * interval.seconds();
                    ps.setString(1, c.symbol());
                    ps.setString(2, interval.name());
                    ps.setLong(3, bucket);
                    ps.setDouble(4, price);
                    ps.setDouble(5, price);
                    ps.setDouble(6, price);
                    ps.setDouble(7, price);
                    ps.setLong(8, 1);
                    ps.addBatch();
                }
            }


            ps.executeBatch();
            conn.commit();
        }
    }
}
