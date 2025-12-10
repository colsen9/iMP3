package com.imp3.Backend.recommendation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recId;

    @Column(nullable = false)
    private Integer senderUid;

    @Column(nullable = false)
    private Integer recipientUid;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String itemId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecSource source;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Privacy privacy = Privacy.PUBLIC;

    public enum RecSource {
        USER_WRITTEN, GEMINI_GENERATED
    }

    public enum Privacy {
        PUBLIC,
        PRIVATE
    }

}
