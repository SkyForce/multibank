package com.multibankfx.model;

public record BidAskEvent(String symbol, double bid, double ask, long timestamp) { }