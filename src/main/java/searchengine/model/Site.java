package searchengine.model;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;
import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "Site", indexes = {
        @Index(name = "unique_name_index", columnList = "name"),
        @Index(name = "unique_url_index", columnList = "url")
})
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time")
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site")
    private List<Page> pages;

    @OneToMany(mappedBy = "site")
    private List<Lemma> lemmas;
}
