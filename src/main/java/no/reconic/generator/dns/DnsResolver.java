package no.reconic.generator.dns;

import java.util.List;

public interface DnsResolver {
    List<String> lookup(String name, String recordType);
}
