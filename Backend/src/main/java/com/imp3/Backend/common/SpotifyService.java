package com.imp3.Backend.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.imp3.Backend.music.TrackDTO;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class SpotifyService {

    @Autowired
    UserRepository userrepository;

    private final String clientId;
    private final String clientSecret;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpotifyService.class);

    private String accessToken;
    private Instant tokenExpiry;
    private static final int MAX_SEEDS = 5;

    public SpotifyService(){
        this.clientId = System.getenv("SPOTIFY_CLIENT_ID");
        this.clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");

        if(clientId == null || clientSecret == null){
            throw new IllegalStateException("Spotify client ID/secret not set in environment variables");
        }
    }

    /**
     *  Get a valid access token, refreshing it if needed
     * @return access token
     */
    private String getAccessToken(){
        if(accessToken == null || tokenExpiry == null || Instant.now().isAfter(tokenExpiry)){
            requestAccessToken();
        }
        return accessToken;
    }

    /**
     * Request a new access token from Spotify using client credentials flow
     */
    private void requestAccessToken(){
        String auth = clientId + ":" + clientSecret;
        String basicAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + basicAuth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                request,
                JsonNode.class
        );

        if(!response.getStatusCode().is2xxSuccessful() || response.getBody() == null){
            throw new IllegalStateException("Failed to get Spotify access token");
        }

        JsonNode json = response.getBody();
        this.accessToken = json.get("access_token").asText();
        int expiresIn = json.get("expires_in").asInt();
        this.tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
    }

    /**
     * Maps Spotify track JSON payload into TrackDTO objects
     * @param payload raw JsonNode containing an array of tracks
     * @return list of TrackDTO representing the returned tracks
     */
    public List<TrackDTO> mapSpotifyTracksToDTO(JsonNode payload){
        List<TrackDTO> result = new ArrayList<>();
        if (payload == null || !payload.isArray()) return result;

        for (JsonNode t : payload){
            String id = t.path("id").asText(null);
            String name = t.path("name").asText(null);
            int duration = t.path("duration_ms").asInt(0);
            int trackNum = t.path("track_number").asInt(0);

            // we aren't storing artists or albums yet, so these stay empty
            Set<Integer> artistIds = new HashSet<>();
            Set<Integer> albumIds = new HashSet<>();

            result.add(new TrackDTO(
                    null,
                    id,
                    name,
                    null,        // albumTitle (null until expanded later)
                    duration,
                    trackNum,
                    null,
                    null,
                    artistIds,
                    albumIds,
                    new HashSet<>()
            ));
        }
        return result;
    }

    /**
     * Fetches full metadata for a specific Spotify track using its spotify track id
     * @param trackId of item to grab data for
     * @return a JSON object containing details such as track name, artists,
     * album information, and duration/popularity
     */
    public JsonNode getTrack(String trackId){
        if(trackId == null || trackId.isBlank() || trackId.equalsIgnoreCase("null") || trackId.equalsIgnoreCase("undefined")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Spotify item id");
        }
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url ="https://api.spotify.com/v1/tracks/" + trackId + "?market=US";

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );
            JsonNode body = response.getBody();
            if(body == null){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Spotify returned empty response for track: " + trackId);
            }
            return body;
        } catch(HttpClientErrorException.NotFound e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Track exists, but not available in Spotify API via client credentials. Track ID: " + trackId);
        } catch(HttpClientErrorException e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error from spotify when fetching track: " + e.getStatusCode());
        }

    }

    /**
     *  Gets top user tracks using their OAuth access token
     * @param userAccessToken Spotify OAuth access token for this user
     * @return JsonNode array of track objects
     */
    public JsonNode getUserTopTracks(String userAccessToken){
        if(userAccessToken == null || userAccessToken.isBlank()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Spotify account not linked for user");
        }

        //limit is always 20
        int limit = 20;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAccessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = "https://api.spotify.com/v1/me/top/tracks?limit=" + limit;

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Spotify error when fetching user top tracks: " + e.getStatusCode()
            );
        }

        JsonNode body = response.getBody();
        if (body == null || !body.has("items") || !body.get("items").isArray()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Spotify returned invalid top tracks payload"
            );
        }

        JsonNode items = body.get("items");
        if (!items.elements().hasNext()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "User has no top tracks"
            );
        }

        return items;

    }


    /**
     *  Gets top user tracks by user
     * @param user to grab top tracks from
     * @return JsonNode array of track objects
     */
    public JsonNode getUserTopTracks(User user){
        String token = user.getSpotifyAccessToken();

        if(user.getSpotifyAccessTokenExpiry() == null || Instant.now().isAfter(user.getSpotifyAccessTokenExpiry())){
            token = refreshUserAccessToken(user);
            userrepository.save(user);
        }

        try{
            return getUserTopTracks(token);
        } catch (ResponseStatusException e){
            if(e.getMessage().contains("401") && user.getSpotifyRefreshToken() != null){
                token = refreshUserAccessToken(user);
                userrepository.save(user);
                return getUserTopTracks(token);
            }
            throw e;
        }
    }

    /**
     *  Refreshes the user's access token for Spotify
     * @param user to refresh
     * @return the refreshed token
     */
    public String refreshUserAccessToken(User user){
        if(user.getSpotifyRefreshToken() == null || user.getSpotifyRefreshToken().isBlank()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No Spotify refresh token available.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", user.getSpotifyRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(
                    "https://accounts.spotify.com/api/token",
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );
        } catch (Exception e ){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to refresh Spotify token");
        }

        JsonNode body = response.getBody();
        if(body == null || !body.hasNonNull("access_token")){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid token refresh response from Spotify");
        }

        String newToken = body.get("access_token").asText();
        int expiresIn = body.get("expires_in").asInt(3600);

        //only update refresh if Spotify sends a new one
        if(body.hasNonNull("refresh_token")){
            user.setSpotifyRefreshToken(body.get("refresh_token").asText());
        }

        user.setSpotifyAccessToken(newToken);
        user.setSpotifyAccessTokenExpiry(Instant.now().plusSeconds(expiresIn));

        return newToken;
    }

    /**
     *  Gets users top Spotify Artists
     * @param user to grab top artists for
     * @return JsonNode of top artists
     */
    public JsonNode getUserTopArtists(User user){
        String token = user.getSpotifyAccessToken();

        if (user.getSpotifyAccessTokenExpiry() == null || Instant.now().isAfter(user.getSpotifyAccessTokenExpiry())) {
            token = refreshUserAccessToken(user);
            userrepository.save(user);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = "https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term";

        try{
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode body = response.getBody();
            if(body == null || !body.has("items") || !body.get("items").isArray()){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Spotify top artists payload");
            }
            return body.get("items");
        } catch(HttpClientErrorException e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error when fetching top artists: " + e.getStatusCode());
        }
    }

    /**
     *  Gets related artists for a given artist
     * @param artistId of the artist to find related artists for
     * @param user the rec is for
     * @return JsonNode of related artists
     */
    public JsonNode getRelatedArtists(String artistId, User user) {
        if (artistId == null || artistId.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid artist id");

        String token = user.getSpotifyAccessToken();
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User has no valid Spotify OAuth token");
        }

        // force refresh if expired
        if (user.getSpotifyAccessTokenExpiry() == null || Instant.now().isAfter(user.getSpotifyAccessTokenExpiry())) {
            token = refreshUserAccessToken(user);
            userrepository.save(user);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(null, headers);


        String url = "https://api.spotify.com/v1/artists/" + artistId + "/related-artists";

        try {

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode body = response.getBody();
            //DEBUG
            //System.out.println("RAW RELATED RESPONSE = " + response.getBody());
            if (body == null || !body.has("artists") || !body.get("artists").isArray())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid related artists payload");
            return body.get("artists");
        } catch (HttpClientErrorException.NotFound e){
            //artist exists in user's history but has no related-artist data --> just skip
            return null;
        } catch(HttpClientErrorException e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error fetching related artists: " + e.getStatusCode());
        }
    }

    /**
     * Gets an artist's top tracks
     * @param artistId for artist
     * @param user the rec is for
     * @return JsonNode of Artist's top tracks
     */
    public JsonNode getArtistTopTracks(String artistId, User user) {
        if (artistId == null || artistId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid artist id");
        }
        String token = user.getSpotifyAccessToken();

        if (user.getSpotifyAccessTokenExpiry() == null || Instant.now().isAfter(user.getSpotifyAccessTokenExpiry())) {
            token = refreshUserAccessToken(user);
            userrepository.save(user);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(null, headers);

        String url = "https://api.spotify.com/v1/artists/" + artistId + "/top-tracks?market=US";

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body == null || !body.has("tracks") || !body.get("tracks").isArray())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid top tracks payload");
            return body.get("tracks");
        } catch(HttpClientErrorException.NotFound e){
            //artist exists in user's history but has no related-artist data --> just skip
            return null;
        } catch (HttpClientErrorException e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error fetching artist top tracks: " + e.getStatusCode());
        }
    }

    /**
     * Get User's recommended tracks
     * @param user to get recommended tracks for
     * @return list of tracks
     */
    public List<TrackDTO> getRecommendedTracks(User user){

        // Step 1 — get user's top artists
        JsonNode topArtists = getUserTopArtists(user);
        if(topArtists == null || !topArtists.isArray() || topArtists.size() == 0){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "User has no top artists — cannot generate recommendations");
        }

        // Step 2 — extract max 5 unique artist IDs
        List<String> artistIds = new ArrayList<>();
        for(JsonNode a : topArtists){
            if(a.has("id")){
                artistIds.add(a.get("id").asText());
                if(artistIds.size() >= 5) break;
            }
        }

        if(artistIds.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to extract artist IDs from top artists payload");
        }

        // Step 3 — pull top tracks for each artist
        Set<String> seen = new HashSet<>();          // dedupe by track ID
        List<TrackDTO> recommendations = new ArrayList<>();

        for(String id : artistIds){
            JsonNode tracks = getArtistTopTracks(id, user);
            if(tracks == null || !tracks.isArray()) continue;

            for(JsonNode t : tracks){
                String trackId = t.path("id").asText(null);
                if(trackId == null || seen.contains(trackId)) continue;
                seen.add(trackId);

                // Build DTO
                String name = t.path("name").asText(null);
                int duration = t.path("duration_ms").asInt(0);
                int trackNum = t.path("track_number").asInt(0);

                recommendations.add(new TrackDTO(
                        null,
                        trackId,
                        name,
                        null,
                        duration,
                        trackNum,
                        null,
                        null,
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>()
                ));
            }
        }

        if(recommendations.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Spotify returned no track data for recommended artists");
        }

        return recommendations;
    }

    /**
     * Search Spotify for tracks matching a query
     * @param query search query
     * @param user the user making the request
     * @return JsonNode containing search results
     */
    public JsonNode searchTracks(String query, User user){
        if(query == null || query.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }

        String token = user.getSpotifyAccessToken();

        if (user.getSpotifyAccessTokenExpiry() == null ||
                Instant.now().isAfter(user.getSpotifyAccessTokenExpiry())) {
            token = refreshUserAccessToken(user);
            userrepository.save(user);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        //append NOT PODCAST NOT AUDIOBOOK to filter out non-music suggestions
        String enhancedQuery = query + " NOT podcast NOT audiobook";

        log.info("Searching Spotify with query: '{}'", enhancedQuery);

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.spotify.com/v1/search")
                .queryParam("q", enhancedQuery)
                .queryParam("type", "track")
                .queryParam("limit", "10")
                .queryParam("market", "US")
                .toUriString();

        log.info("Full URL: {}" , url);

        try{
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                 url,
                 HttpMethod.GET,
                 request,
                 JsonNode.class
            );

            JsonNode body = response.getBody();
            log.info("Spotify response body: {}", body);

            if(body == null || !body.has("tracks")){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid search response from Spotify");
            }

            JsonNode items = body.path("tracks").path("items");
            log.info("Search returned {} items", items.size());
            return items;
        } catch(HttpClientErrorException e){
            log.error("Spotify search error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error searching Spotify: " + e.getStatusCode());
        }
    }

    /**
     * Search Spotify using CLIENT CREDENTIALS (not user token)
     * This is needed because genre: queries don't work with user OAuth tokens
     * @param query search query
     * @return JsonNode containing search results
     */
    public JsonNode searchTracksWithClientCredentials(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }

        // Use app's client credentials token (not user's OAuth token)
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String enhancedQuery = query;

        log.info("Searching Spotify with client credentials, query: '{}'", enhancedQuery);

        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.spotify.com/v1/search")
                .queryParam("q", enhancedQuery)
                .queryParam("type", "track")
                .queryParam("limit", "20")
                .queryParam("market", "US")
                .toUriString();

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            JsonNode body = response.getBody();

            if (body == null || !body.has("tracks") ||
                    !body.path("tracks").has("items")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Invalid search response from Spotify"
                );
            }

            JsonNode items = body.path("tracks").path("items");
            log.info("Client credentials search returned {} items", items.size());

            return items;

        } catch (HttpClientErrorException e) {
            log.error("Spotify search error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error searching Spotify: " + e.getStatusCode());
        }
    }





}
