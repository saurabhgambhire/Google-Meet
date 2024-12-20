package com.google.meet.Services;

import java.util.UUID;

public interface GoogleMeetService {
    String getAuthorizationUrl() throws Exception;

    String createSpace(String code, String state) throws Exception;
}
