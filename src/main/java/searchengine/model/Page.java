package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "Page", indexes = {
        @javax.persistence.Index(name = "unique_path_index", columnList = "path")
})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Lob
    @Column(nullable = false)
    private String content;

    @OneToMany(mappedBy = "page")
    private List<Index> listIndices;
}
