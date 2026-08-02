package no.reconic.generator.dns;

public class DnsLookupException extends RuntimeException {
    public DnsLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
