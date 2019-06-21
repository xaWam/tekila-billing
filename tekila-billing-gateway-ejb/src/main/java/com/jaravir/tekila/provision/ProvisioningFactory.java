package com.jaravir.tekila.provision;

import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;

import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * Created by sajabrayilov on 11/18/2014.
 */
@Singleton
public class ProvisioningFactory {
    @EJB(beanName = "AzertelekomProvisioner", beanInterface = ProvisioningEngine.class)
    private ProvisioningEngine azertelekomProvisioner;
    @EJB(beanName = "BBTVProvisioner", beanInterface = ProvisioningEngine.class)
    private ProvisioningEngine bbtvProvisioner;
    @EJB(beanName = "CitynetProvisioner", beanInterface = ProvisioningEngine.class)
    private ProvisioningEngine citynetProvisioner;

    public synchronized ProvisioningEngine getProvisioningEngine(Subscription subscription) throws ProvisionerNotFoundException {
        ServiceProvider provider = subscription.getService().getProvider();
        ProvisioningEngine provisioner = null;

        if (provider.getId() == 454100) {
            provisioner = azertelekomProvisioner;
        } else if (provider.getId() == 454101 || provider.getId() == 454104) {
            provisioner = bbtvProvisioner;
        } else if (provider.getId() == 454105) {
            provisioner = citynetProvisioner;
        } else if (provider.getId() == 454106) {
            provisioner = citynetProvisioner;
        } else {
            throw new ProvisionerNotFoundException("Cannot find provider");
        }

        return provisioner;
    }

    public synchronized ProvisioningEngine getProvisioningEngine(Service service) throws ProvisionerNotFoundException {
        ServiceProvider provider = service.getProvider();
        ProvisioningEngine provisioner = null;

        if (provider.getId() == 454100) {
            provisioner = azertelekomProvisioner;
        } else if (provider.getId() == 454101) {
            provisioner = bbtvProvisioner;
        } else if (provider.getId() == 454105) {
            provisioner = citynetProvisioner;
        } else if (provider.getId() == 454106) {
            provisioner = citynetProvisioner;
        } else {
            throw new ProvisionerNotFoundException("Cannot find provider");
        }

        return provisioner;
    }

    public synchronized ProvisioningEngine getCitynetProvisioner() {
        ProvisioningEngine provisioner = null;
        provisioner = citynetProvisioner;
        return provisioner;
    }
}
