package com.imp3.Backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spotify")
public class SpotifyAuthController extends AbstractController {

    private final String clientId = System.getenv("SPOTIFY_CLIENT_ID");
    private final String clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");
    private final String redirectUri = System.getenv("SPOTIFY_REDIRECT_URI");
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userrepository;

    /**
     * Redirects the user to Spotify's OAuth authorization page
     * @param response the HTTP response used to send the redirect to Spotify
     * @param session containing "uid"
     * @return null on successful redirect, or error ResponseEntity if not logged in or misconfigured
     * @throws IOException if the redirect cannot be sent
     */
    @GetMapping("/login")
    public ResponseEntity<String> login(HttpServletResponse response, HttpSession session,
                                        @RequestParam(required = false) Integer uid) throws IOException {
        if(clientId == null || clientSecret == null || redirectUri == null){
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Spotify integration not configured");
        }

        if(uid == null) {
            uid = getSessionUid(session);
        }

        if(uid == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Must be logged in to link to Spotify");
        }

        String scopes = URLEncoder.encode("user-read-email user-read-private user-top-read user-library-read playlist-read-private user-follow-read",
                StandardCharsets.UTF_8);

        String url = "https://accounts.spotify.com/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=" + scopes
                + "&state=" + uid;

        log.info("Redirecting to spotify login: {}", url);
        response.sendRedirect(url);
        return null;
    }

    /**
     * Handles Spotify's OAuth callback after user login
     * @param code the authorization code returned by Spotify
     * @param error returned from Spotify if the user denied access
     * @param session the current user session where tokens will be stored
     * @param state saved by the login redirect
     * @return 200 OK on success, or appropriate error response
     */
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam(required = false) String code,
                                           @RequestParam(required = false, name = "error") String error,
                                           @RequestParam(required = false) String state,
                                           HttpSession session,
                                           HttpServletResponse httpResponse ) throws IOException {
        if(error != null){
            log.error("Spotify OAuth error: {}", error);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Spotify authorization failed: " + error);
        }

        if(code == null){
            return ResponseEntity.badRequest().body("Missing authorization code");
        }

        if(state == null){
            return ResponseEntity.badRequest().body("Missing state parameter");
        }

        Integer uid;
        try {
            uid = Integer.parseInt(state);
        } catch(NumberFormatException e){
            return ResponseEntity.badRequest().body("Invalid state parameter");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                request,
                JsonNode.class
        );

        JsonNode body = response.getBody();
        if(body == null || !response.getStatusCode().is2xxSuccessful()){
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Token exchange failed");
        }

        String accessToken = body.get("access_token").asText();
        String refreshToken = body.hasNonNull("refresh_token")
                ? body.get("refresh_token").asText()
                : null;
        int expiresIn = body.get("expires_in").asInt();
        Instant expiry = Instant.now().plusSeconds(expiresIn);

        //get app user from session
        //Integer uid = getSessionUid(session);

        User user = userrepository.findById(uid).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> meRequest = new HttpEntity<>(meHeaders);

        ResponseEntity<JsonNode> meResponse = restTemplate.exchange(
                "https://api.spotify.com/v1/me",
                HttpMethod.GET,
                meRequest,
                JsonNode.class
        );

        JsonNode meBody = meResponse.getBody();
        String spotifyUserId = (meBody != null && meBody.hasNonNull("id"))
                ? meBody.get("id").asText()
                : null;

        user.setSpotifyUserId(spotifyUserId);
        user.setSpotifyAccessToken(accessToken);
        user.setSpotifyAccessTokenExpiry(expiry);

        if(refreshToken != null){
            user.setSpotifyRefreshToken(refreshToken);
        }

        userrepository.save(user);


        session.setAttribute("spotifyAccessToken", accessToken);
        session.setAttribute("spotifyRefreshToken", refreshToken);
        session.setAttribute("spotifyTokenExpiry", Instant.now().plusSeconds(expiresIn));

        httpResponse.sendRedirect("imp3://spotify-callback?success=true&uid=" + uid);
        return null;
    }


    /**
     * Unlinks a Spotify account for a given user
     * @param session containing "uid"
     * @return ResponseEntity<String> status message after unlinking Spotify
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<String> unlinkSpotify(HttpSession session) {
        Integer uid = getSessionUid(session);

        User user = userrepository.findById(uid).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")
        );

        user.setSpotifyAccessToken(null);
        user.setSpotifyRefreshToken(null);
        user.setSpotifyAccessTokenExpiry(null);
        user.setSpotifyUserId(null);
        userrepository.save(user);

        session.removeAttribute("spotifyAccessToken");
        session.removeAttribute("spotifyRefreshToken");
        session.removeAttribute("spotifyTokenExpiry");

        return ResponseEntity.ok("Spotify unlinked. Re-auth at /api/spotify/login");
    }

    /**
     * Returns the current user's Spotify access token, refreshing if needed
     * @param uid user ID
     * @return the access token or error
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken(@RequestParam Integer uid){
        User user  = userrepository.findById(uid)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if(user.getSpotifyRefreshToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Spotify not linked", "needsAuth", true));

    }
        // Check if token is expired or about to expire (within 60 seconds)
        if (user.getSpotifyAccessTokenExpiry() == null ||
                Instant.now().plusSeconds(60).isAfter(user.getSpotifyAccessTokenExpiry())) {

            // Refresh the token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "refresh_token");
            form.add("refresh_token", user.getSpotifyRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        "https://accounts.spotify.com/api/token",
                        HttpMethod.POST,
                        request,
                        JsonNode.class
                );

                JsonNode body = response.getBody();
                if (body == null || !response.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body(Map.of("error", "Token refresh failed"));
                }

                String newAccessToken = body.get("access_token").asText();
                int expiresIn = body.get("expires_in").asInt();

                // Update stored token
                user.setSpotifyAccessToken(newAccessToken);
                user.setSpotifyAccessTokenExpiry(Instant.now().plusSeconds(expiresIn));

                // Spotify may return a new refresh token
                if (body.hasNonNull("refresh_token")) {
                    user.setSpotifyRefreshToken(body.get("refresh_token").asText());
                }

                userrepository.save(user);

            } catch (Exception e) {
                log.error("Failed to refresh Spotify token", e);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Token refresh failed", "needsAuth", true));
            }
        }

        return ResponseEntity.ok(Map.of(
                "accessToken", user.getSpotifyAccessToken(),
                "expiresAt", user.getSpotifyAccessTokenExpiry().toString(),
                "spotifyUserId", user.getSpotifyUserId()
        ));

    }

    /**
     * Proxy search to Spotify API
     * @param uid of the user id (must have Spotify linked)
     * @param q search query
     * @param type type of search artist, track, album, or comma-separated combination
     * @param limit max results to return (default 20)
     * @return Spotify search results or error
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSpotify(
            @RequestParam Integer uid,
            @RequestParam String q,
            @RequestParam(defaultValue = "album,track") String type,
            @RequestParam(defaultValue = "20") int limit) {
        User user = userrepository.findById(uid).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getSpotifyRefreshToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Spotify not linked", "needsAuth", true));
        }

        // Check if token expired or expiring within 60 seconds
        if (user.getSpotifyAccessTokenExpiry() == null ||
                Instant.now().plusSeconds(60).isAfter(user.getSpotifyAccessTokenExpiry())) {

            // Refresh the token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "refresh_token");
            form.add("refresh_token", user.getSpotifyRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        "https://accounts.spotify.com/api/token",
                        HttpMethod.POST,
                        request,
                        JsonNode.class
                );

                JsonNode body = response.getBody();
                if (body == null || !response.getStatusCode().is2xxSuccessful()) {
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body(Map.of("error", "Token refresh failed"));
                }

                user.setSpotifyAccessToken(body.get("access_token").asText());
                user.setSpotifyAccessTokenExpiry(Instant.now().plusSeconds(body.get("expires_in").asInt()));

                if (body.hasNonNull("refresh_token")) {
                    user.setSpotifyRefreshToken(body.get("refresh_token").asText());
                }

                userrepository.save(user);

            } catch (Exception e) {
                log.error("Failed to refresh Spotify token", e);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Token refresh failed", "needsAuth", true));
            }
        }

        // Call Spotify search API
        String url = "https://api.spotify.com/v1/search?q=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8) +
                "&type=" + type +
                "&limit=" + limit;

        HttpHeaders searchHeaders = new HttpHeaders();
        searchHeaders.setBearerAuth(user.getSpotifyAccessToken());

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(searchHeaders),
                    JsonNode.class
            );
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Spotify search failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Spotify search failed"));
        }
    }

    }