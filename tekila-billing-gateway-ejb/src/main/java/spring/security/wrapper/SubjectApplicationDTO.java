package spring.security.wrapper;


import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Application entity.
 */
public class SubjectApplicationDTO implements Serializable {

    private Long id;

    private Integer status;

    private String title;

    private String name;

    private String path;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubjectApplicationDTO that = (SubjectApplicationDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SubjectApplicationDTO{" +
            "id=" + id +
            ", status=" + status +
            ", title='" + title + '\'' +
            ", name='" + name + '\'' +
            ", path='" + path + '\'' +
            '}';
    }
}
