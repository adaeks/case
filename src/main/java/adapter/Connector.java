package adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.AvailableMethods;
import model.LoginStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
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

    private <T> Optional<T> getRequest(final String getUrl, Class<T> clazz) {
        CookieHandler.setDefault(cookieManager);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(getUrl).openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", AUTHORIZATION);
            connection.setRequestProperty("X-Client", X_CLIENT);

            Optional<String> jsessionIdValue = checkForJsessionId();
            if (jsessionIdValue.isPresent())
                connection.setRequestProperty("JSESSIONID", jsessionIdValue.get());

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    ObjectMapper mapper = new ObjectMapper();
                    T response = mapper.readValue(inputStream, clazz);
                    return Optional.of(response);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return Optional.empty();
    }

    private int postRequest(final String postUrl, final String payload) {
        CookieHandler.setDefault(cookieManager);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(postUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", AUTHORIZATION);
            connection.setRequestProperty("X-Client", X_CLIENT);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK)
                return responseCode;

        } catch (IOException e) {
            System.out.println("IOException occurred: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return 0;
    }

    public void getAvailableLoginMethods() {
        Optional<AvailableMethods> optionalAvailableMethods = getRequest(IDENTIFICATION_URL, AvailableMethods.class);
        optionalAvailableMethods.ifPresent(availableMethods ->
                availableMethods.getAuthenticationMethods().stream()
                        .map(AvailableMethods.AuthenticationMethod::getCode)
                        .forEach(System.out::println)
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
                Optional<LoginStatus> optionalLoginStatus = getRequest(VERIFY_URL, LoginStatus.class);
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

