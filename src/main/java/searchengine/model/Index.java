package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Table(name = "index_model", indexes = @javax.persistence.Index(name = "idx_page_lemma", columnList = "page_id, lemma_id"))
@Getter
@Setter
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false, referencedColumnName = "id")
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false, referencedColumnName = "id")
    private Lemma lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;
}