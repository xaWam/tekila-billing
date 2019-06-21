package spring.security.wrapper;


import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Provider entity.
 */
public class SubjectProviderDTO implements Serializable {

    private Long id;

    private Integer status;

    private String name;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubjectProviderDTO providerDTO = (SubjectProviderDTO) o;
        if(providerDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), providerDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "SubjectProviderDTO{" +
            "id=" + id +
            ", status=" + status +
            ", name='" + name + '\'' +
            '}';
    }
}
