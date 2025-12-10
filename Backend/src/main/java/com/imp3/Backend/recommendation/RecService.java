package com.imp3.Backend.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.imp3.Backend.common.GeminiService;
import com.imp3.Backend.common.SpotifyService;
import com.imp3.Backend.user.User;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class RecService {
    private final SpotifyService spotifyservice;
    private final GeminiService geminiservice;

    private static final Logger log = LoggerFactory.getLogger(RecService.class);

    /**
     * Generate personalized discovery recommendations for a user
     * @param user to discover recs for
     * @return list of tracks
     */
    public List<SuggestedTrackResponse> discoverRecommendations(User user){
        //get user's Spotify data
        JsonNode topTracks = spotifyservice.getUserTopTracks(user);
        JsonNode topArtists = spotifyservice.getUserTopArtists(user);

        if(topArtists == null || !topArtists.isArray() || topArtists.isEmpty()){
            return List.of(); // no recs available
        }

        //Prepare data for Gemini (simplified summaries)
        String trackSummary = buildTrackSummary(topTracks);
        String artistSummary = buildArtistSummary(topArtists);

        //ask Gemini to generate search queries based on user taste
        log.info("Asking Gemini to analyze user taste and generate search queries...");
        List<String> searchQueries = geminiservice.generateSearchQueries(
                trackSummary,
                artistSummary
        );

        log.info("Gemini generated {} search queries: {}", searchQueries.size(), searchQueries);

        //execute searches and collect candidate tracks
        Set<String> seenTrackIds = new HashSet<>();
        List<SuggestedTrackResponse> candidates = new ArrayList<>();

        for(String query : searchQueries){
            if(candidates.size() >= 50) break; //get enough candidates

            try{
                JsonNode results = spotifyservice.searchTracksWithClientCredentials(query);

                if(results != null && results.isArray()){
                    for(JsonNode trackNode : results) {
                        String trackId = trackNode.path("id").asText(null);
                        String trackType = trackNode.path("type").asText("");

                        //skip of not track type
                        if (!trackType.equals("track")) {
                            continue;
                        }
                        if (trackId != null && !seenTrackIds.contains(trackId)) {
                            seenTrackIds.add(trackId);

                            String name = trackNode.path("name").asText(null);

                            //skip non-music recs
                            if (name != null && (name.toLowerCase().contains("chapter") || name.toLowerCase().contains("episode"))) {
                                continue;
                            }

                            String artistName = "";
                            if(trackNode.has("artists") && trackNode.path("artists").isArray()
                                    && trackNode.path("artists").size() > 0 ){
                                artistName = trackNode.path("artists").get(0).path("name").asText();
                            }

                            int duration = trackNode.path("duration_ms").asInt(0);

                            //extract preview URL
                            String previewUrl = trackNode.path("preview_url").asText(null);

                            log.info("  -> ADDING to candidates: {} by {}", name, artistName);
                            //map to DTO
                            candidates.add(new SuggestedTrackResponse(
                                    trackId,
                                    name,
                                    artistName,
                                    duration,
                                    previewUrl
                            ));
                        } else {
                            log.info("  -> SKIPPING (duplicate or null ID)");
                        }
                    }
                }
            } catch (Exception e){
                log.warn("Failed to search query: {}", query, e);
            }
        }

        log.info("Total candidates collected: {}", candidates.size());

        //filter out tracks user already knows
        Set<String> knownTrackNames = new HashSet<>();
        if(topTracks != null && topTracks.isArray()){
            for(JsonNode track : topTracks){
                String name = track.path("name").asText("");
                if(!name.isEmpty()) {
                    knownTrackNames.add(name.toLowerCase());
                }
            }
        }

        log.info("Known tracks to filter: {}", knownTrackNames.size());

        List<SuggestedTrackResponse> filtered = candidates.stream()
                .filter(track -> track.name() != null &&
                        !knownTrackNames.contains(track.name().toLowerCase()))
                .limit(15)
                .toList();

        log.info("Returning {} fresh recommendations", filtered.size());
        return filtered;
    }

    /**
     * Build a concise summary of user's top tracks for Gemini
     * @param topTracks of user
     * @return String of summary for user's top tracks
     */
    private String buildTrackSummary(JsonNode topTracks){
        if (topTracks == null || !topTracks.isArray()) return "No top tracks";

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (JsonNode track : topTracks) {
            String name = track.path("name").asText("");
            String artist = track.path("artists").isArray() && track.path("artists").size() > 0
                    ? track.path("artists").get(0).path("name").asText("")
                    : "";

            if (!name.isEmpty()) {
                sb.append(String.format("%s by %s, ", name, artist));
                count++;
                if (count >= 10) break; // Limit to top 10 for token efficiency
            }
        }
        return sb.toString();
    }

    /**
     * Build a concise summary of user's top artists for Gemini
     * @param topArtists of user
     * @return String of summary of user's top artists
     */
    private String buildArtistSummary(JsonNode topArtists){
        if (topArtists == null || !topArtists.isArray()) return "No top artists";

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (JsonNode artist : topArtists) {
            String name = artist.path("name").asText("");
            JsonNode genres = artist.path("genres");

            String genreStr = "";
            if (genres.isArray() && genres.size() > 0) {
                genreStr = " (" + genres.get(0).asText("") + ")";
            }

            if (!name.isEmpty()) {
                sb.append(name).append(genreStr).append(", ");
                count++;
                if (count >= 10) break;
            }
        }
        return sb.toString();
    }

}
