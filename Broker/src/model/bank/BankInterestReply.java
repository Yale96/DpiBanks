package model.bank;

import java.io.Serializable;
import java.security.SecureRandom;

/**
 * This class stores information about the bank reply to a loan request of the
 * specific client
 *
 */
public class BankInterestReply implements Serializable {

    private double interest; // the loan interest
    private String bankId; // the nunique quote Id

    private String hash;

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    public BankInterestReply() {
        this.interest = 0;
        this.bankId = "";
    }

    public BankInterestReply(double interest, String quoteId) {
        this.interest = interest;
        this.bankId = quoteId;
    }

    public double getInterest() {
        return interest;
    }

    public void setInterest(double interest) {
        this.interest = interest;
    }

    public String getQuoteId() {
        return bankId;
    }

    public void setQuoteId(String quoteId) {
        this.bankId = quoteId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String toString() {
        return "quote=" + this.bankId + " interest=" + this.interest;
    }
}
