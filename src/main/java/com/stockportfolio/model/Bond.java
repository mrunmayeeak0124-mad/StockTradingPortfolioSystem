package com.stockportfolio.model;

import com.stockportfolio.exception.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A bond: a loan to an issuer that pays a coupon (interest) until maturity.
 */
public class Bond extends Asset {

    private final String issuer;
    private final BigDecimal faceValue;
    private final BigDecimal couponRate;   // annual rate in percent, e.g. 7.10
    private final LocalDate maturityDate;

    public Bond(String assetId, String name, String issuer, String currency,
                BigDecimal faceValue, BigDecimal couponRate,
                LocalDate maturityDate, BigDecimal currentPrice) {
        super(assetId, name, AssetCategory.BOND, currency, currentPrice);
        if (issuer == null || issuer.isBlank()) {
            throw new InvalidInputException("Issuer cannot be empty.");
        }
        if (faceValue == null || faceValue.signum() <= 0) {
            throw new InvalidInputException("Face value must be greater than zero.");
        }
        if (couponRate == null || couponRate.signum() < 0) {
            throw new InvalidInputException("Coupon rate cannot be negative.");
        }
        if (maturityDate == null) {
            throw new InvalidInputException("Maturity date is required.");
        }
        this.issuer = issuer.trim();
        this.faceValue = faceValue.setScale(2, RoundingMode.HALF_UP);
        this.couponRate = couponRate.setScale(2, RoundingMode.HALF_UP);
        this.maturityDate = maturityDate;
    }

    public String getIssuer() { return issuer; }
    public BigDecimal getFaceValue() { return faceValue; }
    public BigDecimal getCouponRate() { return couponRate; }
    public LocalDate getMaturityDate() { return maturityDate; }

    /** Yearly coupon payment per bond = face value x coupon rate / 100. */
    public BigDecimal getAnnualCouponAmount() {
        return faceValue.multiply(couponRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getSymbol() {
        return getName();
    }

    @Override
    public String getDetails() {
        return "Bond | Issuer: " + issuer + " | Coupon: " + couponRate
                + "% | Matures: " + maturityDate;
    }
}