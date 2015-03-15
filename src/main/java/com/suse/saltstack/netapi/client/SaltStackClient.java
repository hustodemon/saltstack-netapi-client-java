package com.suse.saltstack.netapi.client;

import com.suse.saltstack.netapi.Constants;
import com.suse.saltstack.netapi.client.impl.HttpClientConnectionFactory;
import com.suse.saltstack.netapi.config.ClientConfig;
import com.suse.saltstack.netapi.config.ProxySettings;
import com.suse.saltstack.netapi.datatypes.cherrypy.Stats;
import com.suse.saltstack.netapi.exception.SaltStackException;
import com.suse.saltstack.netapi.parser.JsonParser;
import com.suse.saltstack.netapi.datatypes.JobMinions;
import com.suse.saltstack.netapi.results.Result;
import com.suse.saltstack.netapi.datatypes.Token;

import com.google.gson.JsonArray;

import com.suse.saltstack.netapi.utils.ClientUtils;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * SaltStack API client.
 */
public class SaltStackClient {

    /** The configuration object */
    private final ClientConfig config = new ClientConfig();

    /** The connection factory object */
    private final ConnectionFactory connectionFactory;

    /** The executor for async operations */
    private final ExecutorService executor;

    /**
     * Constructor for connecting to a given URL.
     *
     * @param url the SaltStack URL
     */
    public SaltStackClient(URI url) {
        this(url, new HttpClientConnectionFactory());
    }

    /**
     * Constructor for connecting to a given URL using a specific connection factory.
     *
     * @param url the SaltStack URL
     * @param connectionFactory Connection Factory implementation
     */
    public SaltStackClient(URI url, ConnectionFactory connectionFactory) {
        this(url, connectionFactory, Executors.newCachedThreadPool());
    }

    /**
     * Constructor for connecting to a given URL.
     *
     * @param url the SaltStack URL
     * @param executor Executor for async operations
     */
    public SaltStackClient(URI url, ExecutorService executor) {
        this(url, new HttpClientConnectionFactory(), executor);
    }

    /**
     * Constructor for connecting to a given URL using a specific connection factory.
     *
     * @param url the SaltStack URL
     * @param connectionFactory Connection Factory implementation
     * @param executor Executor for async operations
     */
    public SaltStackClient(URI url, ConnectionFactory connectionFactory,
            ExecutorService executor) {
        // Put the URL in the config
        config.put(ClientConfig.URL, url);
        this.connectionFactory = connectionFactory;
        this.executor = executor;
    }

    /**
     * Directly access the configuration.
     *
     * @return the configuration object
     */
    public ClientConfig getConfig() {
        return config;
    }

    /**
     * Configure to use a proxy when connecting to the SaltStack API.
     *
     * @param settings proxy settings
     */
    public void setProxy(ProxySettings settings) {
        if (settings.getHostname() != null) {
            config.put(ClientConfig.PROXY_HOSTNAME, settings.getHostname());
            config.put(ClientConfig.PROXY_PORT, settings.getPort());
        }
        if (settings.getUsername() != null) {
            config.put(ClientConfig.PROXY_USERNAME, settings.getUsername());
            if (settings.getPassword() != null) {
                config.put(ClientConfig.PROXY_PASSWORD, settings.getPassword());
            }
        }
    }

    /**
     * Perform login and return the token.
     *
     * POST /login
     *
     * @param username the username
     * @param password the password
     * @return authentication token as {@link Token}
     * @throws SaltStackException if anything goes wrong
     */
    public Token login(String username, String password)
            throws SaltStackException {
        return login(username, password, Constants.LOGIN_EAUTH_AUTO);
    }

    /**
     * Perform login and return the token. Allows specifying the eauth parameter.
     *
     * POST /login
     *
     * @param username the username
     * @param password the password
     * @param eauth the eauth type
     * @return authentication token as {@link Token}
     * @throws SaltStackException if anything goes wrong
     */
    public Token login(final String username, final String password, final String eauth)
            throws SaltStackException {
        Map<String, String> props = new LinkedHashMap<String, String>() {
            {
                put("username", username);
                put("password", password);
                put("eauth", eauth);
            }
        };
        Result<List<Token>> result = connectionFactory
                .create("/login", JsonParser.TOKEN, config)
                .getResult(ClientUtils.makeJsonData(props, null, null).toString());

        // For whatever reason they return a list of tokens here, take the first
        Token token = result.getResult().get(0);
        config.put(ClientConfig.TOKEN, token.getToken());
        return token;
    }

    /**
     * Asynchronously perform login and return a Future with the token.
     *
     * POST /login
     *
     * @param username the username
     * @param password the password
     * @return Future containing an authentication token as {@link Token}
     */
    public Future<Token> loginAsync(final String username, final String password) {
        Callable<Token> callable = new Callable<Token>() {
            @Override
            public Token call() throws SaltStackException {
                return login(username, password);
            }
        };
        return executor.submit(callable);
    }

    /**
     * Asynchronously perform login and return a Future with the token.
     * Allows specifying the eauth parameter.
     *
     * POST /login
     *
     * @param username the username
     * @param password the password
     * @param eauth the eauth type
     * @return Future containing an authentication token as {@link Token}
     */
    public Future<Token> loginAsync(final String username, final String password,
            final String eauth) {
        Callable<Token> callable = new Callable<Token>() {
            @Override
            public Token call() throws SaltStackException {
                return login(username, password, eauth);
            }
        };
        return executor.submit(callable);
    }

    /**
     * Perform logout and clear the session token from the config.
     *
     * POST /logout
     *
     * @throws SaltStackException if anything goes wrong
     */
    public Result<String> logout() throws SaltStackException {
        Result<String> result = connectionFactory
                .create("/logout", JsonParser.STRING, config).getResult(null);
        config.remove(ClientConfig.TOKEN);
        return result;
    }

    /**
     * Asynchronously perform logout and clear the session token from the config.
     *
     * POST /logout
     *
     */
    public Future<Result<String>> logoutAsync() {
        Callable<Result<String>> callable = new Callable<Result<String>>() {
            @Override
            public Result<String> call() throws SaltStackException {
                return logout();
            }
        };
        return executor.submit(callable);
    }

    /**
     * Generic interface to start any execution command and immediately return the job id.
     *
     * POST /minions
     *
     * @param target the target
     * @param function the function to execute
     * @param args list of non-keyword arguments
     * @param kwargs map containing keyword arguments
     * @return object representing the scheduled job
     * @throws SaltStackException if anything goes wrong
     */
    public JobMinions startCommand(final String target, final String function,
            List<String> args, Map<String, String> kwargs) throws SaltStackException {
        Map<String, String> props = new LinkedHashMap<String, String>() {
            {
                put("tgt", target);
                put("fun", function);
            }
        };

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(ClientUtils.makeJsonData(props, kwargs, args));

        // Connect to the minions endpoint and send the above lowstate data
        Result<List<JobMinions>> result = connectionFactory
                .create("/minions", JsonParser.JOB_MINIONS,  config)
                .getResult(jsonArray.toString());

        // They return a list of tokens here, we take the first
        return result.getResult().get(0);
    }

    /**
     * Asynchronously start any execution command and immediately return the job id
     *
     * POST /minions
     *
     * @param target the target
     * @param function the function to execute
     * @param args list of non-keyword arguments
     * @param kwargs map containing keyword arguments
     * @return Future containing the scheduled job {@link JobMinions}
     */
    public Future<JobMinions> startCommandAsync(final String target, final String function,
            final List<String> args, final Map<String, String> kwargs) {
        Callable<JobMinions> callable = new Callable<JobMinions>() {
            @Override
            public JobMinions call() throws SaltStackException {
                return startCommand(target, function, args, kwargs);
            }
        };
        return executor.submit(callable);
    }

    /**
     * Query for result of supplied job.
     *
     * GET /job/<job-id>
     *
     * @param job {@link JobMinions} object representing scheduled job
     * @return Map key: minion id, value: command result from that minion
     * @throws SaltStackException if anything goes wrong
     */
    public Map<String, Object> getJobResult(final JobMinions job)
            throws SaltStackException {
        return getJobResult(job.getJid());
    }

    /**
     * Query for result of supplied job.
     *
     * GET /job/<job-id>
     *
     * @param job String representing scheduled job
     * @return Map key: minion id, value: command result from that minion
     * @throws SaltStackException if anything goes wrong
     */
    public Map<String, Object> getJobResult(final String job) throws SaltStackException {
        Result<List<Map<String, Object>>> result = connectionFactory
                .create("/jobs/" + job, JsonParser.RETVALS, config)
                .getResult();

        // A list with one element is returned, we take the first
        return result.getResult().get(0);
    }

    /**
     * Generic interface to start any execution command bypassing normal session handling.
     *
     * POST /run
     *
     * @param username the username
     * @param password the password
     * @param eauth the eauth type
     * @param client the client
     * @param target the target
     * @param function the function to execute
     * @param args list of non-keyword arguments
     * @param kwargs map containing keyword arguments
     * @return Map key: minion id, value: command result from that minion
     * @throws SaltStackException if anything goes wrong
     */
    public Map<String, Object> run(final String username, final String password,
            final String eauth, final String client, final String target,
            final String function, List<String> args, Map<String, String> kwargs)
            throws SaltStackException {
        Map<String, String> props = new LinkedHashMap<String, String>() {
            {
                put("username", username);
                put("password", password);
                put("eauth", eauth);
                put("client", client);
                put("tgt", target);
                put("fun", function);
            }
        };

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(ClientUtils.makeJsonData(props, kwargs, args));

        Result<List<Map<String, Object>>> result = connectionFactory
                .create("/run", JsonParser.RETVALS, config)
                .getResult(jsonArray.toString());

        // A list with one element is returned, we take the first
        return result.getResult().get(0);
    }

    /**
     * Asynchronously start any execution command bypassing normal session handling.
     *
     * POST /run
     *
     * @param username the username
     * @param password the password
     * @param eauth the eauth type
     * @param client the client
     * @param target the target
     * @param function the function to execute
     * @param args list of non-keyword arguments
     * @param kwargs map containing keyword arguments
     * @return Future containing Map key: minion id, value: command result from that minion
     */
    public Future<Map<String, Object>> runAsync(final String username,
            final String password, final String eauth, final String client,
            final String target, final String function, final List<String> args,
            final Map<String, String> kwargs) {
        Callable<Map<String, Object>> callable = new Callable<Map<String, Object>>() {
            @Override
            public Map<String, Object> call() throws SaltStackException {
                return run(username, password, eauth, client,
                        target, function, args, kwargs);
            }
        };
        return executor.submit(callable);
    }

    /**
     * Query statistics from the CherryPy Server.
     *
     * GET /stats
     *
     * @return The {@link Stats} object.
     * @throws SaltStackException if anything goes wrong
     */
    public Stats stats() throws SaltStackException {
        return connectionFactory.create("/stats", JsonParser.STATS, config).getResult();
    }

    /**
     * Asynchronously query statistics from the CherryPy Server.
     *
     * GET /stats
     *
     * @return Future containing the {@link Stats} object.
     */
    public Future<Stats> statsAsync() {
        Callable<Stats> callable = new Callable<Stats>() {
            @Override
            public Stats call() throws SaltStackException {
                return stats();
            }
        };
        return executor.submit(callable);
    }
}
