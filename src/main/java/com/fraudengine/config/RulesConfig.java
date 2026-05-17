package com.fraudengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "rules")
public class RulesConfig {

    private String version;
    private HighAmountConfig highAmount = new HighAmountConfig();
    private VelocityConfig velocity = new VelocityConfig();
    private DeviceFingerprintConfig deviceFingerprint = new DeviceFingerprintConfig();
    private GeolocationConfig geolocation = new GeolocationConfig();
    private OddHoursConfig oddHours = new OddHoursConfig();
    private NewMerchantConfig newMerchant = new NewMerchantConfig();
    private BlacklistConfig blacklist = new BlacklistConfig();

    // getters e setters do root
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public HighAmountConfig getHighAmount() { return highAmount; }
    public void setHighAmount(HighAmountConfig highAmount) { this.highAmount = highAmount; }

    public VelocityConfig getVelocity() { return velocity; }
    public void setVelocity(VelocityConfig velocity) { this.velocity = velocity; }

    public DeviceFingerprintConfig getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(DeviceFingerprintConfig deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public GeolocationConfig getGeolocation() { return geolocation; }
    public void setGeolocation(GeolocationConfig geolocation) { this.geolocation = geolocation; }

    public OddHoursConfig getOddHours() { return oddHours; }
    public void setOddHours(OddHoursConfig oddHours) { this.oddHours = oddHours; }

    public NewMerchantConfig getNewMerchant() { return newMerchant; }
    public void setNewMerchant(NewMerchantConfig newMerchant) { this.newMerchant = newMerchant; }

    public BlacklistConfig getBlacklist() { return blacklist; }
    public void setBlacklist(BlacklistConfig blacklist) { this.blacklist = blacklist; }

    // ── Inner classes ─────────────────────────────────────────

    public static class HighAmountConfig {
        private boolean enabled;
        private BigDecimal threshold;
        private int score;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class VelocityConfig {
        private boolean enabled;
        private int maxTransactions;
        private int windowMinutes;
        private int score;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxTransactions() { return maxTransactions; }
        public void setMaxTransactions(int maxTransactions) { this.maxTransactions = maxTransactions; }

        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class DeviceFingerprintConfig {
        private boolean enabled;
        private int score;
        private int windowMinutes;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
    }

    public static class GeolocationConfig {
        private boolean enabled;
        private int impossibilityWindowMinutes;
        private List<String> highRiskCountries;
        private int scoreImpossibility;
        private int scoreHighRiskCountry;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getImpossibilityWindowMinutes() { return impossibilityWindowMinutes; }
        public void setImpossibilityWindowMinutes(int v) { this.impossibilityWindowMinutes = v; }

        public List<String> getHighRiskCountries() { return highRiskCountries; }
        public void setHighRiskCountries(List<String> highRiskCountries) { this.highRiskCountries = highRiskCountries; }

        public int getScoreImpossibility() { return scoreImpossibility; }
        public void setScoreImpossibility(int scoreImpossibility) { this.scoreImpossibility = scoreImpossibility; }

        public int getScoreHighRiskCountry() { return scoreHighRiskCountry; }
        public void setScoreHighRiskCountry(int scoreHighRiskCountry) { this.scoreHighRiskCountry = scoreHighRiskCountry; }
    }

    public static class OddHoursConfig {
        private boolean enabled;
        private int startHour;
        private int endHour;
        private int score;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getStartHour() { return startHour; }
        public void setStartHour(int startHour) { this.startHour = startHour; }

        public int getEndHour() { return endHour; }
        public void setEndHour(int endHour) { this.endHour = endHour; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class NewMerchantConfig {
        private boolean enabled;
        private BigDecimal minAmountToTrigger;
        private int score;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public BigDecimal getMinAmountToTrigger() { return minAmountToTrigger; }
        public void setMinAmountToTrigger(BigDecimal minAmountToTrigger) { this.minAmountToTrigger = minAmountToTrigger; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class BlacklistConfig {
        private boolean enabled;
        private int score;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }
}