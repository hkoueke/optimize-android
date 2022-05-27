package com.izigo.optimized.models;

public class Transaction {
    private final double trxAmount;
    private final double trxFee;

    public Transaction(final double trxAmount, final double trxFee) {
        this.trxAmount = trxAmount;
        this.trxFee = trxFee;
    }

    public double getTrxAmount() {
        return trxAmount;
    }

    public double getTrxFee() {
        return trxFee;
    }
}