package com.imp3.Backend.tag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TagRequest {
    private String name;
    private Tag.TagCategory category;
    private String description;
}
