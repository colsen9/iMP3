package com.imp3.Backend.list;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ListRequest {
    private Integer userId;
    private String title;
    private String coverImage;
    private String description;
    private UserList.Privacy privacy;
}
