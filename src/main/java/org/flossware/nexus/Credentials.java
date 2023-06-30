package org.flossware.nexus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author sfloess
 */
@Configuration
//@PropertySource("classpath:creds.properties")
public class Credentials {
    @Value("${nexus.url}")
    private String url;

    @Value("${nexus.user}")
    private String user;

    @Value("${nexus.password}")
    private String password;

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
