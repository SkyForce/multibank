package com.multibankfx.service;

import com.multibankfx.model.BidAskEvent;
import com.multibankfx.model.Candle;
import com.multibankfx.model.Interval;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class TimescaleBatchUpsertWriter {

    private final List<Interval> intervals = List.of(new Interval(1, "1s"), new Interval(5, "5s"),
    new Interval(60, "1m"), new Interval(300, "5m"), new Interval(900, "15m"));
    private final DataSource dataSource;

    public TimescaleBatchUpsertWriter(DataSource ds) {
        this.dataSource = ds;
    }

    public void saveInIntervals(List<BidAskEvent> batch) throws Exception {
        for(Interval interval: intervals) {

        }
    }

    public void saveBatch(List<BidAskEvent> batch) throws Exception {
        if (batch.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (BidAskEvent r : batch) {
            double price = (r.ask() + r.bid()) / 2.0;
            Instant instant = Instant.ofEpochSecond(r.timestamp());
            OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
            sb.append(odt).append("\t")
                    .append(r.symbol()).append("\t")
                    .append(price).append("\n");
        }

        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        try (Connection conn = dataSource.getConnection()) {
            CopyManager cm = conn.unwrap(PGConnection.class).getCopyAPI();

            cm.copyIn("""
                COPY ticks(time, symbol, price)
                FROM STDIN WITH (FORMAT text)
            """, stream);
        }
    }
}

