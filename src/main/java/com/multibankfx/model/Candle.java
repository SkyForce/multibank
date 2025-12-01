package com.multibankfx.model;

public record Candle(long time, double open, double high, double low, double close, long volume) {
    public static Candle initial(long time, double price) {
        return new Candle(time, price, price, price, price, 1L);
    }
    public Candle withPrice(double price) {
        double newHigh = Math.max(high, price);
        double newLow  = Math.min(low, price);
        return new Candle(time, open, newHigh, newLow, price, volume + 1);
    }
}