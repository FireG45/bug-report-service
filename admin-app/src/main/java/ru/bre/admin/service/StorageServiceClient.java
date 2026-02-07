package ru.bre.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.bre.admin.model.FeedbackDto;
import ru.bre.admin.model.ReportDto;
import ru.bre.admin.model.SummaryDto;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

public class StorageServiceClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String baseUrl;
    private String reportBaseUrl;
    private String secret;

    public StorageServiceClient() {
        HttpClient client;
        try {
            // Создаем TrustManager, который принимает все сертификаты
            // Это необходимо для работы с IP адресами, у которых нет валидного SSL сертификата
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Отключаем hostname verification (иначе упадет на сертификатах без SAN для IP)
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null);

            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .build();
        } catch (Exception e) {
            // Если не удалось создать SSL контекст, используем стандартный клиент
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        this.httpClient = client;
        this.objectMapper = new ObjectMapper();
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
    }

    public void setReportBaseUrl(String reportBaseUrl) {
        if (reportBaseUrl.endsWith("/")) {
            reportBaseUrl = reportBaseUrl.substring(0, reportBaseUrl.length() - 1);
        }
        this.reportBaseUrl = reportBaseUrl;
    }

    public void setSecret(String secret) {
        this.secret = secret.trim();
    }

    public void delete(String entity) throws IOException, InterruptedException {
        String url = baseUrl + "/v1/api/clean/" + entity + "?secret=" + secret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }
    }

    public List<ReportDto> getReports(int offset, int limit) throws IOException, InterruptedException {
        String url = baseUrl + "/v1/api/report?offset=" + offset + "&limit=" + limit + "&secret=" + secret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<ReportDto>>() {});
    }

    public List<FeedbackDto> getFeedback(int offset, int limit) throws IOException, InterruptedException {
        String url = baseUrl + "/v1/api/feedback?offset=" + offset + "&limit=" + limit + "&secret=" + secret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<FeedbackDto>>() {});
    }

    public List<SummaryDto> getSummary(int offset, int limit) throws IOException, InterruptedException {
        String url = baseUrl + "/v1/api/summary?offset=" + offset + "&limit=" + limit + "&secret=" + secret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<SummaryDto>>() {});
    }

    public boolean getFrontendReport() throws IOException, InterruptedException {
        String url = reportBaseUrl + "/get-frontend-report";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }

        return Boolean.parseBoolean(response.body());
    }

    public String setFrontendReport(boolean value) throws IOException, InterruptedException {
        String url = reportBaseUrl + "/set-frontend-report/" + "?value=" + value + "&secret=" + secret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP error code: " + response.statusCode() + ", response: " + response.body());
        }

        return response.body();
    }
}
