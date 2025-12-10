package com.imp3.Backend.list;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ListSongs", uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "song_id"}))
public class ListSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private UserList list;

    @Column(name = "song_id", nullable = false)
    private Integer songId;
}
