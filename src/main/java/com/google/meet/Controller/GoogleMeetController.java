package com.google.meet.Controller;

import com.google.meet.Services.GoogleMeetService;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller class to handle Google Meet-related API endpoints.
 */
@RestController
@RequestMapping("/api/master/google-meet")
public class GoogleMeetController {

    private final GoogleMeetService googleMeetService;

    /**
     * Constructor for GoogleMeetController.
     *
     * @param googleMeetService the service handling Google Meet operations
     */
    @Autowired
    public GoogleMeetController(GoogleMeetService googleMeetService) {
        this.googleMeetService = googleMeetService;
    }

    /**
     * Generates an authorization URL for Google Meet.
     *
     * @return ResponseEntity containing the authorization URL
     * @throws Exception if any error occurs during URL generation
     */
    @GetMapping("/auth")
    public ResponseEntity<String> getAuthUrl() throws Exception {
        String authorizationUrl = googleMeetService.getAuthorizationUrl();
        return ResponseEntity.ok(authorizationUrl);
    }

    /**
     * Creates a Google Meet space using the provided authorization code and state.
     *
     * @param code  the authorization code from Google OAuth
     * @param state the state parameter to validate the request
     * @return ResponseEntity containing the HTML response or an error message
     */
    @GetMapping("/create-space")
    public ResponseEntity<String> createSpace(@RequestParam String code, @RequestParam String state) {
        try {
            // Call the service method to create a Google Meet space
            String htmlResponse = googleMeetService.createSpace(code, state);

            // Return the HTML response with appropriate content type
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(htmlResponse);
        } catch (Exception e) {
            // Handle errors and return an error response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<html><body><h1>Error creating Google Meet space.</h1></body></html>");
        }
    }
}
