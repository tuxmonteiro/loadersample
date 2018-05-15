package com.globo.grou.loadersample.client;

import com.globo.grou.loadersample.services.LoaderService;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class JettyHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderService.class);

    private JettyHttpClient() { }

    public static void run(URI uri) {
        Executor executor = Executors.newCachedThreadPool();
        HTTP2Client h2Client = new HTTP2Client();
        h2Client.setExecutor(executor);
        HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);
        
        SslContextFactory sslContextFactory = new SslContextFactory(true);

        final HttpClient httpClient = new HttpClient(transport, sslContextFactory);
        httpClient.setFollowRedirects(false);
        httpClient.setCookieStore(new HttpCookieStore.Empty());
        httpClient.setMaxConnectionsPerDestination(1);
        httpClient.setIdleTimeout(10000);

        try {
            httpClient.start();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        long start = System.currentTimeMillis();
        final AtomicInteger counter = new AtomicInteger(0);
        IntStream.rangeClosed(1, 100).parallel().forEach(x -> httpClient.newRequest(uri)
                    .version(HttpVersion.HTTP_2)
                    .listener(new Request.Listener.Adapter() {
                        @Override
                        public void onQueued(Request request) {
                            counter.incrementAndGet();
                        }

                        @Override
                        public void onFailure(Request request, Throwable failure) {
                            LOGGER.error(failure.getMessage(), failure);
                            counter.decrementAndGet();
                        }

                        @Override
                        public void onSuccess(Request request) {
                            counter.decrementAndGet();
                        }
                    })
                    .onResponseContent((response, content) -> {
                        if (LOGGER.isDebugEnabled()) {
                            int status = response.getStatus();
                            LOGGER.info("status : " + status);
                            LOGGER.info("version : " + response.getVersion().asString());
                            //log.info("TLS Protocols : " + Stream.of(r.sslParameters().getProtocols()).collect(Collectors.joining(",")));
                            //log.info("Cyphers : " + Stream.of(r.sslParameters().getCipherSuites()).collect(Collectors.joining(",")));
                            LOGGER.info(new String(content.array()));
                        }
                        LOGGER.warn(">> " + x);
                    }).send(result -> {})
        );
        long interval = System.currentTimeMillis() - start;
        LOGGER.warn("Duration: " + interval);
        LOGGER.warn("Num requests: " + counter.get());

        try {
            while (counter.get() > 0) TimeUnit.SECONDS.sleep(1);
            httpClient.stop();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
