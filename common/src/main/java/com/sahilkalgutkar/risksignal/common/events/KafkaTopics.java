package com.sahilkalgutkar.risksignal.common.events;

/** Topic names shared between producers and consumers so they can't drift out of sync. */
public final class KafkaTopics {

    public static final String TRANSACTIONS = "transactions";
    public static final String RISK_SCORES = "risk-scores";

    private KafkaTopics() {
    }
}
