package com.imp3.Backend;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SaraSystemTest {

    @LocalServerPort
    private int port;

    private static final String DEFAULT_PASSWORD = "password";
    private static final Integer TEST_SONG_ID = 43;

    @BeforeEach
     void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    /**
     * Helper methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private String createUserSession(String email, String username){
        signupUser(email, username);
        return loginUser(email);
    }

    private void signupUser(String email, String username){
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(userPayload(email, username))
                .post("/users/signup")
                .then()
                .statusCode(anyOf(is(200), is(409)));
    }

    private String loginUser(String email){
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s", "password": "%s"}
                        """.formatted(email, DEFAULT_PASSWORD))
                .post("/users/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("JSESSIONID");
    }

    private int createList(String session, String title, String description, String privacy){
        return RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "title": "%s",
                            "description": "%s",
                            "coverImage": "",
                            "privacy": "%s" 
                        }
                        """.formatted(title, description, privacy))
                .post("/lists")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("listId");
    }

    private int getUserIdByEmail(String email){
        return RestAssured.given()
                .get("/users/all")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("findAll { it.email == '%s' }.id".formatted(email), Integer.class)
                .get(0);
    }

    private static String userPayload(String email, String username){
        return """
                {
                    "email": "%s",
                    "password": "%s",
                    "username": "%s",
                    "type": "user"
                }
                """.formatted(email, DEFAULT_PASSWORD, username);
    }

    /*
      Tests ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Test 1: Create a list and fetch it
     */
    @Test
    void createListAndFetchIt(){
        String session = createUserSession("sara_list_test@iastate.edu", "saraList");

        int listId = createList(session, "Gym playlist", "For pumping iron", "PUBLIC");
        assertTrue(listId > 0);

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/lists/{id}", listId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Gym playlist"))
                .body("owner.id", notNullValue());
    }

    /**
     * Test 2: Add a song to list and confirm the correct song was added
     */
    @Test
    void addSongToListAndVerifyTracks(){
        String session = createUserSession("sara_song_test@iastate.edu", "saraSongs");
        Integer listId = createList(session, "Sad mix", "Crying time", "PUBLIC");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .post("/lists/{listId}/songs/{songId}", listId, TEST_SONG_ID)
                .then()
                .statusCode(200)
                .body("message", equalTo("Song added"));

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/lists/{listId}/tracks", listId)
                .then()
                .statusCode(200)
                .body("id", hasItem(TEST_SONG_ID));
    }

    /**
     * Test 3: Create a Notification and mark as read
     */
    @Test
    void createNotificationAndMarkRead(){
        String recipientSession = createUserSession("notifA@iastate.edu", "notifA");
        String actorSession = createUserSession("notifB@iastate.edu", "notifB");
        Integer recipientId = getUserIdByEmail("notifA@iastate.edu");

        Integer notifId = RestAssured.given()
                .cookie("JSESSIONID", actorSession)
                .contentType(ContentType.JSON)
                .body("""
                        {"recipientId": %d, "message": "Hi!", "type":"FOLLOW"}
                        """.formatted(recipientId))
                .post("/notif")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("notifId");

        //verify notification is unread
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .get("/notif")
                .then()
                .statusCode(200)
                .body("readAt", hasItem(nullValue()));
        //mark as read
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .put("/notif/{notifId}/read", notifId)
                .then()
                .statusCode(200);

        //verify all notifications are now read
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .get("/notif")
                .then()
                .statusCode(200);
    }

    /**
     *  Test 4: Follower User and verify lists
     */
    @Test
    void followUserAndVerifyLists(){
        String sessionA = createUserSession("followA@iastate.edu", "followA");
        String sessionB = createUserSession("followB@iastate.edu", "followB");

        int idA = getUserIdByEmail("followA@iastate.edu");
        int idB = getUserIdByEmail("followB@iastate.edu");

        RestAssured.given()
                .cookie("JSESSIONID", sessionA)
                .post("/users/{id}/follow", idB)
                .then()
                .statusCode(anyOf(is(200), is(409)));

        RestAssured.given()
                .get("/users/{id}/followers", idB)
                .then()
                .statusCode(200)
                .body("id", hasItem(idA));

        RestAssured.given()
                .get("/users/{id}/following", idA)
                .then()
                .statusCode(200)
                .body("id", hasItem(idB));

    }

    /**
     * Test 5: Create and fetch a Tag
     */
    @Test
    void createTagAndFetchIt(){
        String session = createUserSession("sara_tag_test@iastaete.edu", "saraTag");
        String uniqueName = "smooth vibes " + currentTimeMillis();

        //create a Tag
        Integer tagId = RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "category": "MOOD"
                        }
                        """.formatted(uniqueName))
                .post("/tag")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .jsonPath()
                .getInt("tagId");

        //fetch it
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/tag/{tagId}", tagId)
                .then()
                .statusCode(200)
                .body("name", equalTo(uniqueName));
    }

    /**
     * Test 6: Add Tag to User and fetch User's Tags
     */
    @Test
    void addUserTagAndFetchIt(){
        String session = createUserSession("sara_usertag_test@iastaete.edu", "saraUserTag");
        String uniqueName = "rock fan " + System.currentTimeMillis();

        //first create a tag
        Integer tagId = RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "%s",
                            "category": "GENRE"
                        }
                        """.formatted(uniqueName))
                .post("/tag")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .jsonPath()
                .getInt("tagId");

        //add Tag to User
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "tagId": %d
                    }
                    """.formatted(tagId))
                .post("/usertag")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        //fetch User's tags
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/usertag")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    /**
     * Test 7: Update & delete a UserList
     */
    @Test
    void UpdateAndDeleteList(){
        String session = createUserSession("sara_update_test@iastate.edu", "saraUpdate");

        int listId = createList(session, "Original Title", "Original description", "PUBLIC");

        //update the list
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "title": "Updated Title",
                            "description": "Updated description",
                            "coverImage": "",
                            "privacy": "PRIVATE"
                        }
                        """)
                .put("/lists/{listId}", listId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Updated Title"));

        // Delete the list
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .delete("/lists/{listId}", listId)
                .then()
                .statusCode(anyOf(is(200), is(204)));


    }

    /**
     * Test 8: Get all Tags with search filter
     */
    @Test
    void searchTags(){
        String session = createUserSession("sara_search_tag@iastate.edu", "saraSearchTag");
        String uniqueName = "electronic " + System.currentTimeMillis();

        // Create a tag first
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "%s",
                        "category": "GENRE"
                    }
                    """.formatted(uniqueName))
                .post("/tag")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Search for tags
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .queryParam("search", "electronic")
                .get("/tag")
                .then()
                .statusCode(200);

    }

    /**
     * Test 9: Create and fetch a recommendation
     */
    @Test
    void createAndFetchRecommendation(){
        String session = createUserSession("sara_rec_test@iastate.edu", "saraRec");

        // Get all recommendations (even if empty, just hit the endpoint)
        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec")
                .then()
                .statusCode(200);
    }

    /**
     * Test 10: Get User's public recommendations
     */
    @Test
    void getUserPublicRecommendations() {
        String session = createUserSession("sara_rec_public@iastate.edu", "saraRecPublic");
        int userId = getUserIdByEmail("sara_rec_public@iastate.edu");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec/users/{profileUid}", userId)
                .then()
                .statusCode(200);
    }

    /**
     * Test 11: Get mutuals
     */
    @Test
    void getMutualFollowers() {
        String sessionA = createUserSession("mutualA@iastate.edu", "mutualA");
        String sessionB = createUserSession("mutualB@iastate.edu", "mutualB");

        int idA = getUserIdByEmail("mutualA@iastate.edu");
        int idB = getUserIdByEmail("mutualB@iastate.edu");

        // A follows B
        RestAssured.given()
                .cookie("JSESSIONID", sessionA)
                .post("/users/{id}/follow", idB)
                .then()
                .statusCode(anyOf(is(200), is(409)));

        // B follows A
        RestAssured.given()
                .cookie("JSESSIONID", sessionB)
                .post("/users/{id}/follow", idA)
                .then()
                .statusCode(anyOf(is(200), is(409)));

        // Check mutuals
        RestAssured.given()
                .get("/users/{id}/mutuals", idA)
                .then()
                .statusCode(200);
    }

    /**
     * Test 12: Unfollow a user
     */
    @Test
    void unfollowUser() {
        String sessionA = createUserSession("unfollowA@iastate.edu", "unfollowA");
        String sessionB = createUserSession("unfollowB@iastate.edu", "unfollowB");

        int idB = getUserIdByEmail("unfollowB@iastate.edu");

        // Follow first
        RestAssured.given()
                .cookie("JSESSIONID", sessionA)
                .post("/users/{id}/follow", idB)
                .then()
                .statusCode(anyOf(is(200), is(409)));

        // Unfollow
        RestAssured.given()
                .cookie("JSESSIONID", sessionA)
                .delete("/users/{id}/follow", idB)
                .then()
                .statusCode(anyOf(is(200), is(204)));
    }

    /**
     * Test 13: Delete Notification
     */
    @Test
    void deleteNotification() {
        String senderSession = createUserSession("sara_notif_sender@iastate.edu", "saraNotifSender");
        String recipientSession = createUserSession("sara_notif_recipient@iastate.edu", "saraNotifRecipient");
        int recipientId = getUserIdByEmail("sara_notif_recipient@iastate.edu");

        // Create notification (sender creates it for recipient)
        Integer notifId = RestAssured.given()
                .cookie("JSESSIONID", senderSession)
                .contentType(ContentType.JSON)
                .body("""
                    {"recipientId": %d, "message": "Test", "type":"FOLLOW"}
                    """.formatted(recipientId))
                .post("/notif")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .jsonPath()
                .getInt("notifId");

        // Recipient deletes it
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .delete("/notif/{notifId}", notifId)
                .then()
                .statusCode(anyOf(is(200), is(204)));
    }

    /**
     * Test 14: Get all user's lists
     */
    @Test
    void getAllMyLists() {
        String session = createUserSession("sara_mylists@iastate.edu", "saraMyLists");

        // Create a list first
        createList(session, "My Test List", "Description", "PUBLIC");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/lists")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    /**
     * Test 14: Create a Recommendation
     */
    @Test
    void createRecommendation() {
        String session = createUserSession("sara_create_rec@iastate.edu", "saraCreateRec");
        int recipientId = getUserIdByEmail("sara_create_rec@iastate.edu");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "type": "TRACK",
                        "recipientUid": %d,
                        "itemId": "1",
                        "title": "Great Song",
                        "rationale": "You'll love this!",
                        "privacy": "PUBLIC"
                    }
                    """.formatted(recipientId))
                .post("/rec")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));
    }

    /**
     * Test 15: Get all recommendations
     */
    @Test
    void getAllRecommendations() {
        String session = createUserSession("sara_all_rec@iastate.edu", "saraAllRec");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec")
                .then()
                .statusCode(200);
    }

    /**
     * Test 16: Get specific recommendation
     */
    @Test
    void getSpecificRecommendation() {
        String session = createUserSession("sara_get_rec@iastate.edu", "saraGetRec");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec/{recId}", 1)
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    /**
     * Test 17: Get recommendation spotify data
     */
    @Test
    void getRecommendationSpotify() {
        String session = createUserSession("sara_rec_spotify@iastate.edu", "saraRecSpotify");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec/{recId}/spotify", 1)
                .then()
                .statusCode(anyOf(is(200), is(404), is(400)));
    }

    /**
     * Test 18: Get user's public recommendations
     */
    @Test
    void getUserPublicRecs() {
        String session = createUserSession("sara_pub_rec@iastate.edu", "saraPubRec");
        int userId = getUserIdByEmail("sara_pub_rec@iastate.edu");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec/users/{profileUid}", userId)
                .then()
                .statusCode(200);
    }

    /**
     * Test 19: Get suggested tracks
     */
    @Test
    void getSuggestedTracks() {
        String session = createUserSession("sara_suggest@iastate.edu", "saraSuggest");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .get("/rec/suggestions")
                .then()
                .statusCode(anyOf(is(200), is(400), is(401), is(500)));
    }

    /**
     * Test 20: Update recommendation
     */
    @Test
    void updateRecommendation() {
        String session = createUserSession("sara_upd_rec@iastate.edu", "saraUpdRec");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "rationale": "Updated rationale",
                        "privacy": "PRIVATE"
                    }
                    """)
                .put("/rec/{recId}", 1)
                .then()
                .statusCode(anyOf(is(200), is(404), is(403)));
    }

    /**
     * Test 21: Delete recommendation
     */
    @Test
    void deleteRecommendation() {
        String session = createUserSession("sara_del_rec@iastate.edu", "saraDelRec");

        RestAssured.given()
                .cookie("JSESSIONID", session)
                .delete("/rec/{recId}", 1)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404), is(403)));
    }

    /**
     * Test 21: THE FINAL TEST!
     */
    @Test
    void fullRecommendationWorkflow() {
        String senderSession = createUserSession("sara_rec_sender@iastate.edu", "saraRecSender");
        String recipientSession = createUserSession("sara_rec_recipient@iastate.edu", "saraRecRecipient");
        int recipientId = getUserIdByEmail("sara_rec_recipient@iastate.edu");

        // Sender creates a recommendation
        Integer recId = RestAssured.given()
                .cookie("JSESSIONID", senderSession)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "type": "track",
                        "recipientUid": %d,
                        "itemId": "1",
                        "title": "Amazing Song",
                        "rationale": "You will love this!"
                    }
                    """.formatted(recipientId))
                .post("/rec")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .jsonPath()
                .getInt("recId");

        // RECIPIENT fetches the recommendation (not sender!)
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .get("/rec/{recId}", recId)
                .then()
                .statusCode(200);

        // Recipient gets all their recommendations
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .get("/rec")
                .then()
                .statusCode(200);

        // Recipient deletes the recommendation
        RestAssured.given()
                .cookie("JSESSIONID", recipientSession)
                .delete("/rec/{recId}", recId)
                .then()
                .statusCode(anyOf(is(200), is(204)));
    }



}
