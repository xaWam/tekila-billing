package spring.dto;

import com.jaravir.tekila.module.service.entity.ServiceProvider;

/**
 * Created by KamranMa on 25.12.2017.
 */
public class ServiceProviderResponse {
    public final String name;
    public final Long id;

    private ServiceProviderResponse(final ServiceProvider provider) {
        this.name = provider.getName();
        this.id = provider.getId();
    }

    public static ServiceProviderResponse from(final ServiceProvider provider) {
        return new ServiceProviderResponse(provider);
    }
}
