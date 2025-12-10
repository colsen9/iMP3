package com.imp3.Backend.list;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ListAlbums", uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "album_id"}))
public class ListAlbum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private UserList list;

    @Column(name = "album_id", nullable = false)
    private Integer albumId;
}
