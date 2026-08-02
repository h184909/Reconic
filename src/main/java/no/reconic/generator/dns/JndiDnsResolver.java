package no.reconic.generator.dns;

import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Component
public class JndiDnsResolver implements DnsResolver {

    private static final String DNS_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String INITIAL_TIMEOUT_MS = "1500";
    private static final String RETRIES = "1";

    @Override
    public List<String> lookup(String name, String recordType) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, DNS_FACTORY);
        environment.put("com.sun.jndi.dns.timeout.initial", INITIAL_TIMEOUT_MS);
        environment.put("com.sun.jndi.dns.timeout.retries", RETRIES);

        InitialDirContext context = null;
        try {
            context = new InitialDirContext(environment);
            Attributes attributes = context.getAttributes("dns:/" + name, new String[]{recordType});
            Attribute attribute = attributes.get(recordType);
            if (attribute == null) {
                return List.of();
            }

            List<String> values = new ArrayList<>();
            NamingEnumeration<?> enumeration = attribute.getAll();
            while (enumeration.hasMore()) {
                Object value = enumeration.next();
                if (value != null) {
                    values.add(value.toString());
                }
            }
            return List.copyOf(values);
        } catch (NameNotFoundException exception) {
            return List.of();
        } catch (NamingException exception) {
            throw new DnsLookupException(
                    "DNS-oppslag feilet for " + name + " (" + recordType + ")",
                    exception
            );
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // Nothing useful can be done during cleanup.
                }
            }
        }
    }
}
