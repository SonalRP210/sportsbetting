package com.sonal.sportsbetting.config;

public final class AppConstants {

    private AppConstants() {
    }

    public static final class Api {
        public static final String BASE_V1 = "/api/v1";
        public static final String BETS = "/bets";
        public static final String BET_BY_ID = "/bets/{betId}";
        public static final String BET_CANCEL = "/bets/{betId}/cancel";
        public static final String ODDS_FEED = "/odds-feed";
        public static final String EVENT_SETTLEMENTS = "/events/settlements";
        public static final String EVENT_BETS = "/events/{eventId}/bets";
        public static final String USER_BETS = "/users/{userId}/bets";
        public static final String USER_EXPOSURE = "/users/{userId}/exposure";
        public static final String HEALTH = "/health";

        private Api() {
        }
    }

    public static final class Messages {
        public static final String ODDS_FEED_ACCEPTED = "accepted";
        public static final String HEALTH_UP = "UP";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String BET_NOT_FOUND = "BET_NOT_FOUND";
        public static final String RATE_LIMITED = "RATE_LIMITED";
        public static final String SETTLEMENT_CONFLICT = "SETTLEMENT_CONFLICT";
        public static final String TRANSIENT_FAILURE = "TRANSIENT_FAILURE";
        public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
        public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";

        private Messages() {
        }
    }
}
