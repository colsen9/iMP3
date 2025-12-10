package com.imp3.Backend;

// springboot classes
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import io.restassured.RestAssured;
import io.restassured.response.Response;

// local classes
import com.imp3.Backend.chat.Chat;
import com.imp3.Backend.music.Track;
import com.imp3.Backend.songoftheday.SongOfTheDay;
import com.imp3.Backend.music.Album;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PennySystemTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /**
     *  Song of the Day
     */
    @Test
    void songOfTheDayTest() {

        // login
        Response login = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"penny@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");

        // check we logged in correctly
        int statusCode = login.getStatusCode();
        assertEquals(200, statusCode);

        // keep the http session alive with cookies (just like how penny stays alive with cookies)
        var cookies = login.getCookies();

        // get today's SotD
        Response getToday = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/sotd/today");
        statusCode = getToday.getStatusCode();
        assertEquals(200, statusCode);

        // get yesterday's SotD
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Response getYesterday = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/sotd/" + yesterday);
        statusCode = getYesterday.getStatusCode();
        assertEquals(200, statusCode);

        // get all SotD
        Response getAll = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/sotd/all");
        statusCode = getAll.getStatusCode();
        assertEquals(200, statusCode);

        // give current song of the day an upvote, which will succeed or fail depending on how many times the test has run today
        Response upvote = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("").
                when().
                put("/sotd/upvote");
        statusCode = upvote.getStatusCode();
        assertTrue(statusCode == 200 || statusCode == 409);

        // vote again, which should fail
        Response doubleVote = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("").
                when().
                put("/sotd/downvote");
        statusCode = doubleVote.getStatusCode();
        assertEquals(409, statusCode);

        // post a SotD in the far future
        Response postSotD = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("").
                when().
                post("/sotd/2026-12-12/44");
        statusCode = postSotD.getStatusCode();
        assertEquals(200, statusCode);

        // delete that SotD
        Response deleteSotD = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("").
                when().
                delete("/sotd/2026-12-12");
        statusCode = deleteSotD.getStatusCode();
        assertEquals(200, statusCode);


    }

    /**
     *  Reviews
     */
    @Test
    void reviewTest() {

        // login
        Response login = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("{ \"email\":\"penny@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");

        // check we logged in correctly
        int statusCode = login.getStatusCode();
        assertEquals(200, statusCode);

        // keep the http session alive with cookies (just like how penny stays alive with cookies)
        var cookies = login.getCookies();

        // get a track from the music catalog
        Response trackResponse = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/tracks/44");

        // data to keep track of
        Integer trackId = (Integer)44;
        Integer albumId = trackResponse.jsonPath().getList("albumIds", Integer.class).get(0);

        // check that so far so good
        statusCode = trackResponse.getStatusCode();
        assertEquals(200, statusCode);

        // post a review of that track
        Response reviewPostResponse = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("{\n" +
                        "\t\"userId\": \"104\",\n" +
                        "\t\"albumId\": \"" + albumId + "\",\n" +
                        "\t\"songId\": \"44\",\n" +
                        "\t\"rating\": \"10\",\n" +
                        "\t\"review\": \"test review\"\n" +
                        "}").
                when().
                post("/reviews/new");
        Integer reviewId = (Integer)reviewPostResponse.jsonPath().getInt("reviewId");
        statusCode = reviewPostResponse.getStatusCode();
        assertEquals(200, statusCode);

        // edit that review
        Response reviewPutResponse = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("{\n" +
                        "\t\"id\": \"" + reviewId + "\",\n" +
                        "\t\"userId\": \"104\",\n" +
                        "\t\"albumId\": \"" + albumId + "\",\n" +
                        "\t\"songId\": \"44\",\n" +
                        "\t\"rating\": \"10\",\n" +
                        "\t\"review\": \"test edit for testing\"\n" +
                        "}").
                when().
                put("/reviews/edit");
        statusCode = reviewPutResponse.getStatusCode();
        assertEquals(200, statusCode);

        // get the reviews for that album only
        Response reviewGetAlbum = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/albums/" + albumId);
        statusCode = reviewGetAlbum.getStatusCode();
        assertEquals(200, statusCode);

        // get the reviews for that album and the songs within it
        Response reviewGetAlbumAll = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/albums/" + albumId + "/all");
        statusCode = reviewGetAlbumAll.getStatusCode();
        assertEquals(200, statusCode);

        // get the reviews for that track
        Response reviewGetTrack = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/tracks/" + trackId);
        statusCode = reviewGetTrack.getStatusCode();
        assertEquals(200, statusCode);

        // get reviews from my user
        Response reviewGetUser = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/users/104");
        statusCode = reviewGetUser.getStatusCode();
        assertEquals(200, statusCode);

        // get reviews of the album by my user
        Response reviewAlbumGetUser = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/albums/" + albumId + "/104");
        statusCode = reviewAlbumGetUser.getStatusCode();
        assertEquals(200, statusCode);

        // get reviews of the track by my user
        Response reviewTrackGetUser = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/tracks/" + trackId + "/104");
        statusCode = reviewTrackGetUser.getStatusCode();
        assertEquals(200, statusCode);

        // get the specific review
        Response reviewSpecific = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/" + reviewId);
        statusCode = reviewSpecific.getStatusCode();
        assertEquals(200, statusCode);

        // get highest reviews
        Response reviewBest = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/best");
        statusCode = reviewBest.getStatusCode();
        assertEquals(200, statusCode);

        // get lowest reviews
        Response reviewWorst = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/reviews/worst");
        statusCode = reviewWorst.getStatusCode();
        assertEquals(200, statusCode);

        // delete the review we wrote earlier
        Response reviewDeleteResponse = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                delete("/reviews/delete/" + reviewId);
        statusCode = reviewDeleteResponse.getStatusCode();
        assertEquals(200, statusCode);
    }

    /**
     *  Music Catalog
     */
    @Test
    void postMusicTest() {
        // login
        Response login = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"penny@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");

        // check we logged in correctly
        int statusCode = login.getStatusCode();
        assertEquals(200, statusCode);

        // keep the http session alive with cookies (just like how penny stays alive with cookies)
        var cookies = login.getCookies();

        // make a new artist
        Response postArtist = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"name\":\"test artist #68.5\" }").
                when().
                post("/music/artists/new");
        statusCode = postArtist.getStatusCode();
        assertEquals(200, statusCode);

        // get the new artist's ID
        Integer artistId = (Integer)postArtist.jsonPath().getInt("artistId");

        // edit the artist
        Response editArtist = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"id\": " + artistId + ", \"name\":\"test artist #421\" }").
                when().
                put("/music/artists/edit");
        statusCode = editArtist.getStatusCode();
        assertEquals(200, statusCode);

        // delete the artist
        Response deleteArtist = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                when().
                delete("/music/artists/delete/" + artistId);
        statusCode = deleteArtist.getStatusCode();
        assertEquals(200, statusCode);

        // get a big list of all tracks
        Response getAllTracks = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/tracks");
        statusCode = getAllTracks.getStatusCode();
        assertEquals(200, statusCode);

        artistId = 18;
        Integer albumId = (Integer)8;
        Integer trackId = (Integer)44;

        // get all singles by one artist
        Response getSingles = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/artists/" + artistId + "/tracks");
        statusCode = getSingles.getStatusCode();
        assertEquals(200, statusCode);

        // get all tracks in one album
        Response getAlbumTracks = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/albums/" + albumId + "/tracks");
        statusCode = getAlbumTracks.getStatusCode();
        assertEquals(200, statusCode);

        // get a specific track
        Response getOneTrack = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/tracks/" + trackId);
        statusCode = getOneTrack.getStatusCode();
        assertEquals(200, statusCode);

        // search for tracks
        Response searchTracks = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/search/tracks/Ce");
        statusCode = searchTracks.getStatusCode();
        assertEquals(200, statusCode);

        // get a list of all albums
        Response getAlbums = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/albums");
        statusCode = getAlbums.getStatusCode();
        assertEquals(200, statusCode);

        // get all albums by an artist
        Response getAlbumsByArtist = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/artists/" + artistId + "/albums");
        statusCode = getAlbumsByArtist.getStatusCode();
        assertEquals(200, statusCode);

        // get specific album information
        Response getAlbumInfo = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/albums/" + albumId);
        statusCode = getAlbumInfo.getStatusCode();
        assertEquals(200, statusCode);

        // search for an album
        Response searchAlbums = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/search/albums/Oh");
        statusCode = searchAlbums.getStatusCode();
        assertEquals(200, statusCode);

        // get a list of all artists
        Response getArtists = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/artists/");
        statusCode = getArtists.getStatusCode();
        assertEquals(200, statusCode);

        // get a specific artist's information
        Response getOneArtist = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/artists/" + artistId);
        statusCode = getOneArtist.getStatusCode();
        assertEquals(200, statusCode);

        // search for an artist
        Response searchArtist = RestAssured.given().
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/music/search/artists/Port");
        statusCode = searchArtist.getStatusCode();
        assertEquals(200, statusCode);

        // ========================================================
        // implement the below only if we haven't reached 70%
        // ========================================================

        // make a new artist

        // make a new track, attach to the artist

        // edit that track

        // delete that track

        // make a new album

        // make a new track, attach to the album

        // edit that track

        // delete that track

        // delete that album

        // delete that artist
    }

    /**
     *  Chat (Controller, not websockets...)
     */
    @Test
    void chatTest() {
        // login
        Response login = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"penny@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");

        // check we logged in correctly
        int statusCode = login.getStatusCode();
        assertEquals(200, statusCode);

        // keep the http session alive with cookies (just like how penny stays alive with cookies)
        var cookies = login.getCookies();

        // get all the messages we've sent
        Response getSent = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/chat/104/sent");
        statusCode = getSent.getStatusCode();
        assertEquals(200, statusCode);

        // get all messages we've received
        Response getReceived = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/chat/104/received");
        statusCode = getReceived.getStatusCode();
        assertEquals(200, statusCode);

        // POST a chat (?)
        // this is only feasible if the returned json has a chatId in it

        // PUT a chat (?)

        // DELETE a chat

    }

    /**
     *  Spotify and Gemini
     */
    @Test
    void apiTest() {

        // SpotifyAuthController - login()
        Response spotifyLogin = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                get("/api/spotify/login");
        int statusCode = spotifyLogin.getStatusCode();
        assertEquals(401, statusCode);

        // SpotifyAuthController - callback()
        Response spotifyCallback = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                get("/api/spotify/callback");
        statusCode = spotifyCallback.getStatusCode();
        assertEquals(400, statusCode);

        // SpotifyAuthController - unlinkSpotify()
        Response spotifyUnlink = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                delete("/api/spotify/unlink");
        statusCode = spotifyUnlink.getStatusCode();
        assertEquals(401, statusCode);

        // SpotifyAuthController - getToken()
        Response spotifyToken = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                get("/api/spotify/token");
        statusCode = spotifyToken.getStatusCode();
        assertEquals(400, statusCode);

        // SpotifyAuthController - searchSpotify()
        Response spotifySearch = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                get("/api/spotify/search");
        statusCode = spotifySearch.getStatusCode();
        assertEquals(400, statusCode);

    }

    /**
     *  Lists and Stuff
     */
    @Test
    void listTest() {

        int statusCode;

        // odds and ends in ListController
        Response listDelete = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                delete("/lists/324562346/songs/234523");
        statusCode = listDelete.getStatusCode();
        assertEquals(401, statusCode);

        // odds and ends in ListController
        Response listDeleteAlbum = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                delete("/lists/324562346/albums/234523");
        statusCode = listDeleteAlbum.getStatusCode();
        assertEquals(401, statusCode);

        // odds and ends in TagController
        Response tagEdit = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                put("/tag/4354573");
        statusCode = tagEdit.getStatusCode();
        assertEquals(400, statusCode);

        // odds and ends in TagController
        Response tagDelete = RestAssured.given().
                header("Content-Type", "application/json").
                when().
                delete("/tag/4354573");
        statusCode = tagEdit.getStatusCode();
        assertEquals(400, statusCode);

    }

    /**
     *  Users
     */
    @Test
    void userTest() {

        // sign up a new user
        Response signup = RestAssured.given().
                header("Content-Type", "application/json").
                body("""
                        {
                        \t"email": "pennyminitestuser@iastate.edu",
                        \t"username": "pennyminitestuser",
                        \t"type": "user",
                        \t"password": "heck"
                        }""").
                when().
                post("/users/signup");
        int statusCode = signup.getStatusCode();
        assertEquals(200, statusCode);

        // log in wrong
        Response loginWrong = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"pennyminitestuser@iastate.edu\", \"password\":\"heckstober\"  }").
                when().
                post("/users/login");
        statusCode = loginWrong.getStatusCode();
        assertEquals(401, statusCode);

        // log in right
        Response loginRight = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"pennyminitestuser@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");
        statusCode = loginRight.getStatusCode();
        assertEquals(200, statusCode);
        Integer userId = (Integer)loginRight.jsonPath().getInt("user");

        // keep the http session alive with cookies (just like how penny stays alive with cookies)
        var cookies = loginRight.getCookies();

        // edit user's data
        Response getToday = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("{\n" +
                        "\t\"userId\": \"" + userId + "\",\n" +
                        "\t\"email\": \"pennyminitestuser@iastate.edu\",\n" +
                        "\t\"bio\": \"writing a bio no one will ever read :(\",\n" +
                        "\t\"firstname\": \"sleepy\"\n" +
                        "}").
                when().
                put("/users/" + userId);
        statusCode = getToday.getStatusCode();
        assertEquals(200, statusCode);

        // get a list of all users
        Response getUserList = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/users/all");
        statusCode = getUserList.getStatusCode();
        assertEquals(200, statusCode);

        // get penny's profile picture (because we don't have one)
        Response prettyPenny = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/users/104/picture");
        statusCode = prettyPenny.getStatusCode();
        assertEquals(200, statusCode);

        // change our password
        Response putPassword = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                body("{\n" +
                        "\t\"password_old\": \"heck\",\n" +
                        "\t\"password_new\": \"hecktoberfest\",\n" +
                        "}").
                when().
                put("/users/" + userId + "/password");
        statusCode = getToday.getStatusCode();
        assertEquals(200, statusCode);

        // search for all the pennies
        Response pennySearch = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                get("/users/search/pen");
        statusCode = getToday.getStatusCode();
        assertEquals(200, statusCode);

        // log in as an admin
        Response adminLogin = RestAssured.given().
                header("Content-Type", "application/json").
                body("{ \"email\":\"penny@iastate.edu\", \"password\":\"heck\"  }").
                when().
                post("/users/login");
        statusCode = adminLogin.getStatusCode();
        assertEquals(200, statusCode);
        var adminCookies = adminLogin.getCookies();

        // ban the test account
        Response banTest = RestAssured.given().
                cookies(adminCookies).
                header("Content-Type", "application/json").
                when().
                put("/users/ban/" + userId);
        statusCode = banTest.getStatusCode();
        assertEquals(200, statusCode);

        // unban the test account
        Response unbanTest = RestAssured.given().
                cookies(adminCookies).
                header("Content-Type", "application/json").
                when().
                put("/users/unban/" + userId);
        statusCode = unbanTest.getStatusCode();
        assertEquals(200, statusCode);

        // delete the test account
        Response deleteUser = RestAssured.given().
                cookies(cookies).
                header("Content-Type", "application/json").
                header("charset","utf-8").
                when().
                delete("/users/" + userId);
        statusCode = getToday.getStatusCode();
        assertEquals(200, statusCode);
    }
}
