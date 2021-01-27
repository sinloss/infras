package com.sinlo.security.jwt.nimbus.spec;

import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.sinlo.core.http.Fetcha;

import java.io.IOException;
import java.net.URL;

/**
 * A {@link ResourceRetriever} especially for retrieving remote resources using the assigned
 * {@link com.sinlo.core.http.Fetcha.Course}
 *
 * @author sinlo
 */
public class RemoteRetriever implements ResourceRetriever {

    private final Fetcha.Course<String> course;

    public RemoteRetriever(Fetcha.Course<String> course) {
        this.course = course;
    }

    @Override
    public Resource retrieveResource(URL url) throws IOException {
        return new Resource(course.get(url.toString())
                .header("Accept",
                        "application/json", "application/jwk-set+json")
                .fetch().join(), "");
    }
}
