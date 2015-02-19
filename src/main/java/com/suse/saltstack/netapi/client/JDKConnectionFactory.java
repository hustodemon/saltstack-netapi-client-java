package com.suse.saltstack.netapi.client;

import com.suse.saltstack.netapi.config.ClientConfig;
import com.suse.saltstack.netapi.parser.SaltStackParser;

/**
 * Implementation of a factory for connections using JDK's HttpURLConnection.
 *
 * @see JDKConnection
 */
public class JDKConnectionFactory implements ConnectionFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> JDKConnection<T> create(String endpoint, SaltStackParser<T> parser, ClientConfig config) {
        return new JDKConnection<>(endpoint, parser, config);
    }
}
