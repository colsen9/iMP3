package com.imp3.Backend.recommendation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecRequest {
    private String type;
    private Integer recipientUid;
    private String itemId;
    private String title;
    private String rationale;
    private Recommendation.Privacy privacy;
    private String spotifyData;
}
