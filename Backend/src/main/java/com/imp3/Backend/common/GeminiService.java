package com.imp3.Backend.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {
    private static final String GENERATION_MODEL = "gemini-2.5-flash";
    private static final String JSON_MODEL = "models/gemini-2.5-flash";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";

    private final Client client;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeminiService.class);

    public GeminiService(){
        this.client = new Client();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Calls Gemini via the official Java SDK and returns plain text
     * @param prompt non-empty prompt string
     * @return model response text (empty string if Gemini returns no text)
     */
    public String generate(String prompt){
        if(prompt == null || prompt.isBlank()){
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        GenerateContentConfig config = GenerateContentConfig.builder().build();

        GenerateContentResponse response = client.models.generateContent(GENERATION_MODEL, prompt, config);

        if(response.text() == null || response.text().isBlank()){
            return "";
        }
        return response.text();
    }

    /**
     *  Calls Gemini with the given prompt & returns raw text response
     * @param prompt non-empty prompt string
     * @return text from the first candidate/part (should be JSON string)
     */
    public String generateJson(String prompt){
        if(prompt == null || prompt.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini prompt must not be empty");
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        if(apiKey == null || apiKey.isBlank()){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini integration is not configured on the server");
        }

        String url = GEMINI_BASE_URL + JSON_MODEL + ":generateContent?key=" + apiKey;

        //build request body
        String body;
        try {
            body ="""
                    {
                      "contents": [
                        {
                          "parts": [
                            { "text": %s }
                          ]
                        }
                      ]
                    }
                    """.formatted(objectMapper.writeValueAsString(prompt));
        } catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize Gemini prompt", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try{
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            JsonNode root = response.getBody();
            if(root == null){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from Gemini");
            }

            JsonNode candidates = root.path("candidates");
            if(!candidates.isArray() || candidates.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No candidates returned by Gemini");
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No content parts returned by Gemini");
            }

            String text = parts.get(0).path("text").asText(null);
            if(text == null || text.isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned empty text");
            }

            return text;
        } catch (HttpClientErrorException e){
            System.err.println("Gemini call failed: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini HTTP error: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error calling Gemini" +  e.getMessage(), e);
        }
    }


    /**
     * Ask Gemini to analyze user's music taste & generate Spotify search queries
     * @param topTracksJson JSON string of User's top tracks
     * @param topArtistsJson JSON string of user's top artists
     * @return JSON array of search query strings
     */
    public List<String> generateSearchQueries(String topTracksJson, String topArtistsJson){
        String prompt = String.format("""
        You are a music recommendation expert. Analyze this user's Spotify listening data and generate 5-7 diverse Spotify search queries to discover NEW music they haven't heard.
        User's Top Tracks:
        %s
        User's Top Artists:
        %s
        Based on their taste, generate search queries using Spotify's search syntax:
        - PRIORITIZE artist searches using "artist:X" (e.g., "artist:Radiohead")  
        - For genres, use ONLY simple, common, single-word genres: indie, rock, pop, electronic, jazz, folk, metal, classical, ambient, blues
        - DO NOT use multi-word genres
        - Focus on WELL-KNOWN artists with simple names (no special characters like ö, ð, ø, etc.)
        - Mix popular and indie artists for variety
    
        Return ONLY a JSON array of 8-10 query strings, nothing else. Example format:
        ["artist:Radiohead", "artist:Arcade Fire", "genre:indie", "artist:MGMT", "genre:electronic"]
    
        Your response:
        """, topTracksJson, topArtistsJson);

        String response = generateJson(prompt);
        //parse the JSON array from Gemini's response
        try {
            //remove markdown code blocks if present
            String cleaned = response.replaceAll("```json|```", "").trim();
            JsonNode jsonArray = objectMapper.readTree(cleaned);

            List<String> queries =new ArrayList<>();
            if(jsonArray.isArray()){
                for(JsonNode node : jsonArray){
                    queries.add(node.asText());
                }
            }
            return queries;
        } catch (JsonProcessingException e){
            log.error("Failed to parse Gemini search queries: {}", response, e);
            //fallback queries (for now)
            return List.of("genre:indie", "genre:alternative", "genre:electronic");
        }
    }

    /**
     * Generate a personalized rationale for why a track is recommended to the user
     * @param trackName name of recommended track
     * @param artistName artist of the track
     * @param userTopData brief summary of user's listening history
     * @return 1-2 sentence rationale
     */
    public String generateRecommendationRationale(String trackName, String artistName, String userTopData){
        String prompt = String.format("""
                A user who listens to: %s
                
                We're recommending the track "%s" by %s.
                
                In 1-2 sentences, explain why this track would appeal to them based on their taste. Be specific and enthusiastic.
                """, userTopData, trackName, artistName);

        return generate(prompt);
    }


}
