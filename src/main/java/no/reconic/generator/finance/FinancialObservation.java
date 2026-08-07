package no.reconic.generator.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FinancialObservation(
        FinancialLookupStatus status,
        String sourceOrganizationNumber,
        boolean sourceIsParent,
        Integer fiscalYear,
        String periodEnd,
        String currency,
        BigDecimal operatingRevenue,
        BigDecimal operatingResult,
        BigDecimal preTaxResult,
        BigDecimal annualResult,
        BigDecimal equity,
        BigDecimal assets,
        BigDecimal debt,
        BigDecimal currentAssets,
        BigDecimal currentLiabilities,
        Boolean unrevisedAnnualAccounts,
        Boolean auditWaived,
        String warning
) {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    public FinancialObservation {
        status = status == null ? FinancialLookupStatus.SKIPPED : status;
        sourceOrganizationNumber = normalize(sourceOrganizationNumber);
        periodEnd = normalize(periodEnd);
        currency = normalize(currency);
        warning = normalize(warning);
    }

    public static FinancialObservation empty() {
        return skipped(null);
    }

    public static FinancialObservation skipped(String reason) {
        return new FinancialObservation(
                FinancialLookupStatus.SKIPPED,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reason
        );
    }

    public static FinancialObservation notAvailable(String organizationNumber, String reason) {
        return new FinancialObservation(
                FinancialLookupStatus.NOT_AVAILABLE,
                organizationNumber,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reason
        );
    }

    public static FinancialObservation failed(String organizationNumber, String reason) {
        return new FinancialObservation(
                FinancialLookupStatus.FAILED,
                organizationNumber,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reason
        );
    }

    public FinancialObservation withSourceIsParent(boolean parent) {
        return new FinancialObservation(
                status,
                sourceOrganizationNumber,
                parent,
                fiscalYear,
                periodEnd,
                currency,
                operatingRevenue,
                operatingResult,
                preTaxResult,
                annualResult,
                equity,
                assets,
                debt,
                currentAssets,
                currentLiabilities,
                unrevisedAnnualAccounts,
                auditWaived,
                warning
        );
    }

    public boolean hasData() {
        return status == FinancialLookupStatus.SUCCESS;
    }

    public boolean hasOperatingRevenue() {
        return operatingRevenue != null;
    }

    public boolean hasPositiveOperatingResult() {
        return operatingResult != null && operatingResult.signum() > 0;
    }

    public boolean hasNegativeOperatingResult() {
        return operatingResult != null && operatingResult.signum() < 0;
    }

    public boolean hasPositiveAnnualResult() {
        return annualResult != null && annualResult.signum() > 0;
    }

    public boolean isNok() {
        return currency == null || "NOK".equalsIgnoreCase(currency);
    }

    public BigDecimal operatingMarginPercent() {
        return percent(operatingResult, operatingRevenue);
    }

    public BigDecimal equityRatioPercent() {
        return percent(equity, assets);
    }

    public BigDecimal currentRatio() {
        if (currentAssets == null || currentLiabilities == null || currentLiabilities.signum() == 0) {
            return null;
        }
        return currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal revenuePerEmployee(int employees) {
        if (operatingRevenue == null || employees <= 0) {
            return null;
        }
        return operatingRevenue.divide(BigDecimal.valueOf(employees), 0, RoundingMode.HALF_UP);
    }

    public String operatingRevenueDisplay() {
        return amountDisplay(operatingRevenue);
    }

    public String operatingResultDisplay() {
        return amountDisplay(operatingResult);
    }

    public String preTaxResultDisplay() {
        return amountDisplay(preTaxResult);
    }

    public String annualResultDisplay() {
        return amountDisplay(annualResult);
    }

    public String equityDisplay() {
        return amountDisplay(equity);
    }

    public String debtDisplay() {
        return amountDisplay(debt);
    }

    public String operatingMarginDisplay() {
        return decimalDisplay(operatingMarginPercent(), " %");
    }

    public String equityRatioDisplay() {
        return decimalDisplay(equityRatioPercent(), " %");
    }

    public String currentRatioDisplay() {
        return decimalDisplay(currentRatio(), "");
    }

    public String revenuePerEmployeeDisplay(int employees) {
        BigDecimal value = revenuePerEmployee(employees);
        if (value == null) {
            return "–";
        }
        return amountDisplay(value);
    }

    public String sourceDisplay() {
        if (sourceOrganizationNumber == null) {
            return "Regnskapsregisteret";
        }
        return sourceIsParent
                ? "Hovedenhet " + sourceOrganizationNumber
                : "Org.nr. " + sourceOrganizationNumber;
    }

    private String amountDisplay(BigDecimal value) {
        if (value == null) {
            return "–";
        }

        BigDecimal millions = value.divide(ONE_MILLION, 1, RoundingMode.HALF_UP);
        String suffix = currency == null ? "" : " " + currency;
        return norwegianDecimal(millions) + " mill." + suffix;
    }

    private String decimalDisplay(BigDecimal value, String suffix) {
        if (value == null) {
            return "–";
        }
        return norwegianDecimal(value) + suffix;
    }

    private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.multiply(ONE_HUNDRED)
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private static String norwegianDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
