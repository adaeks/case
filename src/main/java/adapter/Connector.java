package adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.AvailableMethods;
import model.LoginStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class Connector {
    // HEADERS
    private static final String AUTHORIZATION = "QjdkWkhRY1k3OFZSVno5bDoxNjc4ODgwMDE3OTM1";
    private static final String X_CLIENT = "fdp-internet-bank/199.0.0";

    // URL
    private static final String VERIFY_URL = "https://online.swedbank.se/TDE_DAP_Portal_REST_WEB/api/v5/identification/bankid/mobile/verify";
    private static final String LOGIN_URL = "https://online.swedbank.se/TDE_DAP_Portal_REST_WEB/api/v5/identification/bankid/mobile";
    private static final String IDENTIFICATION_URL = "https://online.swedbank.se/TDE_DAP_Portal_REST_WEB/api/v5/identification/";

    // OTHERS
    private final int POLL_COUNT = 10;
    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    private AvailableMethods getAvailableMethods() {

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IDENTIFICATION_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", AUTHORIZATION)
                .header("X-Client", X_CLIENT)
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                try (InputStream inputStream = response.body()) {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(inputStream, AvailableMethods.class);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        }

        return null;
    }

    private <T> T getRequest(String getUrl, Class<T> clazz) {

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();


        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", AUTHORIZATION)
                .header("X-Client", X_CLIENT)
                .GET();

        checkForJsessionId().ifPresent(jsessionId ->
                requestBuilder.header("JSESSIONID", jsessionId));

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                try (InputStream inputStream = response.body()) {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(inputStream, clazz);
                }
            } else {
                System.out.println("Status code: " + statusCode);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        }

        return null;
    }

    private int postRequest(final String postUrl, final String payload) {

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", AUTHORIZATION)
                .header("X-Client", X_CLIENT)
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            if (responseCode == 200) {
                return responseCode;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Exception occurred: " + e.getMessage());
        }

        return 0;
    }

    public void getAvailableLoginMethods() {
        Optional<AvailableMethods> optionalAvailableMethods = Optional.ofNullable(getAvailableMethods());
        optionalAvailableMethods.ifPresentOrElse(availableMethods ->
                        availableMethods.getAuthenticationMethods().stream()
                                .map(AvailableMethods.AuthenticationMethod::getCode)
                                .forEach(System.out::println),
                () -> System.out.println("No available methods found")
        );
    }

    public void initiateMobileBankIdLogin(final String customerNr) {
        String body = "{\"bankIdOnSameDevice\":\"false\",\"generateEasyLoginId\":\"false\",\"useEasyLogin\":\"false\",\"userId\":\"";
        String payload = body.concat(customerNr+"\"}");

        if(postRequest(LOGIN_URL, payload) != 200) {
            System.out.println("Failed to initiate mobile BankID login");
            return;
        }

        try {
            for (int i = 0; i < POLL_COUNT; i++) {
                Optional<LoginStatus> optionalLoginStatus = Optional.ofNullable(getRequest(VERIFY_URL, LoginStatus.class));
                if(optionalLoginStatus.isEmpty()) {
                    System.out.println("Failed to get login status");
                    break;
                }
                System.out.println(optionalLoginStatus.get().getStatus());
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Failed while polling for login status: " + e.getMessage());
        }
    }

    private Optional<String> checkForJsessionId() {
        Optional<String> optionalValue = Optional.empty();
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID"))
                optionalValue = Optional.of(cookie.getValue());
        }

        return optionalValue;
    }
}

