package com.google.meet.Implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.apps.meet.v2.CreateSpaceRequest;
import com.google.apps.meet.v2.Space;
import com.google.apps.meet.v2.SpacesServiceClient;
import com.google.apps.meet.v2.SpacesServiceSettings;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ClientId;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserAuthorizer;
import com.google.meet.Services.GoogleMeetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

/**
 * Implementation of the GoogleMeetService interface to handle Google Meet integration.
 */
@Service
@Slf4j
public class GoogleMeetServiceImpl implements GoogleMeetService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.token-url}")
    private String tokenUrl;

    private final RestTemplate restTemplate;

    /**
     * Constructor for GoogleMeetServiceImpl.
     *
     * @param restTemplate the RestTemplate to handle HTTP requests
     */
    @Autowired
    public GoogleMeetServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves the authorization URL for Google OAuth.
     *
     * @return the authorization URL as a String
     * @throws Exception if an error occurs while generating the URL
     */
    @Override
    public String getAuthorizationUrl() throws Exception {
        try {
            return getAuthUrl();
        } catch (Exception e) {
            log.warn("Error generating auth URL, retrying.", e);
        }
        return null;
    }

    /**
     * Generates the authorization URL with the necessary scopes and callback.
     *
     * @return the generated authorization URL as a String
     * @throws Exception if an error occurs during URL generation
     */
    private String getAuthUrl() throws Exception {
        UserAuthorizer authorizer = UserAuthorizer.newBuilder()
                .setClientId(ClientId.newBuilder().setClientId(clientId).build())
                .setCallbackUri(URI.create(redirectUri))
                .setScopes(Arrays.asList(
                        "https://www.googleapis.com/auth/meetings.space.created",
                        "https://www.googleapis.com/auth/meetings",
                        "https://www.googleapis.com/auth/calendar",
                        "https://www.googleapis.com/auth/userinfo.email"
                ))
                .build();

        URL authorizationUrl = authorizer.getAuthorizationUrl("user", null, null);
        log.info("Generated Authorization URL: {}", authorizationUrl);
        return authorizationUrl.toString();
    }

    /**
     * Exchanges the authorization code for an access token.
     *
     * @param code the authorization code from Google OAuth
     * @return the access token as a String
     * @throws Exception if an error occurs during token exchange
     */
    private String getAccessToken(String code) throws Exception {
        if (StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractAccessToken(response.getBody());
        } else {
            log.error("Failed to retrieve access token. Response: {}", response.getBody());
            throw new Exception("Failed to retrieve access token.");
        }
    }

    /**
     * Extracts the access token from the JSON response body.
     *
     * @param responseBody the JSON response body
     * @return the access token as a String
     * @throws IOException if an error occurs during JSON parsing
     */
    private String extractAccessToken(String responseBody) throws IOException {
        return new ObjectMapper().readTree(responseBody).path("access_token").asText();
    }

    /**
     * Refreshes the access token using the provided refresh token.
     *
     * @param refreshToken the refresh token
     * @return the new access token as a String
     * @throws Exception if an error occurs during token refresh
     */
    public String refreshAccessToken(String refreshToken) throws Exception {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return extractAccessToken(response.getBody());
        } else {
            throw new Exception("Failed to refresh access token.");
        }
    }

    /**
     * Creates a Google Meet space using the provided authorization code or access token.
     *
     * @param code          the authorization code from Google OAuth
     * @param accToken      an existing access token (optional)
     * @return the HTML response or Google Meet space URI
     * @throws Exception if an error occurs during space creation
     */
    @Override
    public String createSpace(String code, String accToken) throws Exception {
        log.info("Creating Google Meet space with code: {}", code);

        String accessToken = StringUtils.isEmpty(accToken) ? getAccessToken(code) : accToken;
        log.info("Access token: {}", accessToken);

        try {
            GoogleCredentials credentials = GoogleCredentials.newBuilder()
                    .setAccessToken(AccessToken.newBuilder().setTokenValue(accessToken).build())
                    .build();

            SpacesServiceSettings settings = SpacesServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            SpacesServiceClient spacesServiceClient = SpacesServiceClient.create(settings);
            CreateSpaceRequest request = CreateSpaceRequest.newBuilder()
                    .setSpace(Space.newBuilder().build())
                    .build();

            Space space = spacesServiceClient.createSpace(request);
            log.info("Successfully created space with URI: {}", space.getMeetingUri());

            String meetingUri = space.getMeetingUri();
            return StringUtils.isEmpty(accToken) ? generateHtmlResponse(meetingUri) : meetingUri;
        } catch (Exception e) {
            log.error("Error creating Google Meet space.", e);
            throw new Exception("Error creating Google Meet space.");
        }
    }

    /**
     * Generates an HTML response with an embedded Google Meet iframe.
     *
     * @param meetingUri the URI of the Google Meet space
     * @return the HTML response as a String
     */
    private String generateHtmlResponse(String meetingUri) {
        return """
            <html>
            <head>
                <meta http-equiv="refresh" content="0; url=%s" />
                <title>Google Meet</title>
            </head>
            <body>
                <div style="text-align: center;">
                    <h1 style="font-family: inter;">Joining Google Meet...</h1>
                    <iframe src="%s" width="600" height="400" allow="camera; microphone" style="border: 0;"></iframe>
                </div>
            </body>
            </html>
        """.formatted(meetingUri, meetingUri);
    }
}