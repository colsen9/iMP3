package com.imp3.Backend.tag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserTagRequest {
    private Integer tagId;
    private UserTag.TagType tagType;
    private UserTag.Privacy privacy;
    private Double confidence;
    private String source;
}
