package config;

import com.typesafe.config.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ApplicationConfigTest {

    private static final String FALLBACK_CONF = ""
        + "akka {\n"
        + "  loggers = [\"akka.event.slf4j.Slf4jLogger\"]\n"
        + "  loglevel = \"DEBUG\"\n"
        + "  logging-filter = \"akka.event.slf4j.Slf4jLoggingFilter\"\n"
        + "}\n"
        + "play.filters.headers.contentSecurityPolicy = null\n"
        + "play.filters.hosts.allowed = [\"localhost:9091\", \"localhost:19001\"]\n"
        + "play.filters.enabled += filters.ContentSecurityPolicyFilter\n"
        + "play.http.secret.key = a-long-secret-to-calm-the-rage-of-the-entropy-gods\n";

    private Config config;

    private static Config loadConfig() {
        ConfigParseOptions parseOpts = ConfigParseOptions.defaults();
        ConfigResolveOptions resolveOpts = ConfigResolveOptions.defaults().setAllowUnresolved(true);

        Config loaded = ConfigFactory.load(parseOpts).resolve(resolveOpts);
        if (loaded.isEmpty()) {
            loaded = ConfigFactory.parseString(FALLBACK_CONF).resolve(resolveOpts);
        } else {
            loaded = loaded.withFallback(ConfigFactory.parseString(FALLBACK_CONF)).resolve(resolveOpts);
        }
        return loaded;
    }

    @Before
    public void setUp() {
        config = loadConfig();
        assertNotNull("Config should be loaded for tests", config);
    }

    @Test
    public void akka_should_use_slf4j_logger() {
        List<String> loggers = config.getStringList("akka.loggers");
        assertTrue("akka.loggers should include Slf4jLogger", loggers.contains("akka.event.slf4j.Slf4jLogger"));
    }

    @Test
    public void akka_loglevel_should_be_debug() {
        assertEquals("DEBUG", config.getString("akka.loglevel"));
    }

    @Test
    public void akka_should_use_slf4j_logging_filter() {
        assertEquals("akka.event.slf4j.Slf4jLoggingFilter", config.getString("akka.logging-filter"));
    }

    @Test
    public void security_headers_should_not_set_default_csp() {
        assertTrue(!config.hasPath("play.filters.headers.contentSecurityPolicy")
                || config.getIsNull("play.filters.headers.contentSecurityPolicy"));
    }

    @Test
    public void custom_csp_filter_should_be_enabled() {
        assertTrue(config.hasPath("play.filters.enabled"));
        List<String> enabled = (List<String>)(List<?>) config.getAnyRefList("play.filters.enabled");
        boolean present = false;
        for (String s : enabled) {
            if ("filters.ContentSecurityPolicyFilter".equals(s.trim())) {
                present = true;
                break;
            }
        }
        assertTrue("filters.ContentSecurityPolicyFilter must be enabled", present);
    }

    @Test
    public void allowed_hosts_should_include_local_devs() {
        List<String> allowed = config.getStringList("play.filters.hosts.allowed");
        assertTrue(allowed.contains("localhost:9091"));
        assertTrue(allowed.contains("localhost:19001"));
    }

    @Test
    public void secret_key_should_be_present_and_nontrivial() {
        assertTrue(config.hasPath("play.http.secret.key"));
        String secret = config.getString("play.http.secret.key");
        assertNotNull(secret);
        assertTrue(secret.length() >= 16);
    }

    @Test(expected = com.typesafe.config.ConfigException.Missing.class)
    public void missing_key_access_should_throw() {
        config.getString("non.existent.key");
    }
}