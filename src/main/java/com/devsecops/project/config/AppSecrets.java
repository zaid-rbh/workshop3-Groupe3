package com.devsecops.project.config;

/**
 * VULNERABILITY #18: hard-coded secrets in source code.
 * All values below are FAKE and exist only for the educational demonstration.
 */
public final class AppSecrets {

    private AppSecrets() {
    }

    /** Fake API key committed to source control. */
    public static final String API_KEY = "FAKE-API-KEY-sk_test_1234567890abcdef";

    /** Fake static AES key (16 bytes) reused for every encryption. See CryptoService. */
    public static final String AES_KEY = "0123456789abcdef";

    /** Fake backup administrator password. */
    public static final String ADMIN_BACKUP_PASSWORD = "admin123";
}
