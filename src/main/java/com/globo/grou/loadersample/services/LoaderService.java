package com.globo.grou.loadersample.services;

import com.globo.grou.loadersample.client.JettyHttpClient;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class LoaderService {

    public LoaderService() {
        String uriTemp = System.getenv("TARGET");
        URI uri = uriTemp != null ? URI.create(uriTemp) : URI.create("https://http2.pro/api/v1");
        JettyHttpClient.run(uri);
    }

}
