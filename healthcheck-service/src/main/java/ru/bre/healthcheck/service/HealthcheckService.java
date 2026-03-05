package ru.bre.healthcheck.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import ru.bre.healthcheck.model.ServiceStatus;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@ConfigurationProperties(prefix = "healthcheck")
public class HealthcheckService {

    private List<Map<String, String>> services = new ArrayList<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void setServices(List<Map<String, String>> services) {
        this.services = services;
    }

    public List<Map<String, String>> getServices() {
        return services;
    }

    public List<ServiceStatus> checkAll() {
        List<CompletableFuture<ServiceStatus>> futures = services.stream()
                .map(svc -> CompletableFuture.supplyAsync(() -> checkService(svc)))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private ServiceStatus checkService(Map<String, String> svc) {
        String name = svc.get("name");
        String url = svc.get("url");
        String host = svc.get("host");
        String port = svc.get("port");

        if (url != null) {
            boolean up = checkHttp(url);
            return new ServiceStatus(name, up ? "up" : "down", url);
        } else if (host != null && port != null) {
            String target = host + ":" + port;
            boolean up = checkTcp(host, Integer.parseInt(port));
            return new ServiceStatus(name, up ? "up" : "down", target);
        }

        return new ServiceStatus(name, "down", "unknown");
    }

    private boolean checkHttp(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkTcp(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
