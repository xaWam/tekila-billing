package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.Streets;

/**
 * Created by kmaharov on 22.05.2017.
 */
public class AgreementGenerationParams {
    public final String agreement;
    public final String ats;
    public final Streets str;
    public final String building;
    public final String apartment;
    public final String msisdn;

    private AgreementGenerationParams(
            String agreement,
            String ats,
            Streets str,
            String building,
            String apartment,
            String msisdn
    ) {
        this.agreement = agreement;
        this.ats = ats;
        this.str = str;
        this.building = building;
        this.apartment = apartment;
        this.msisdn = msisdn;
    }

    public static class Builder {
        private String agreement;
        private String ats;
        private Streets str;
        private String building;
        private String apartment;
        private String msisdn;

        public Builder() {}

        public Builder setAgreement(String agreement) {
            this.agreement = agreement;
            return this;
        }

        public Builder setAts(String ats) {
            this.ats = ats;
            return this;
        }

        public Builder setStr(Streets str) {
            this.str = str;
            return this;
        }

        public Builder setBuilding(String building) {
            this.building = building;
            return this;
        }

        public Builder setApartment(String apartment) {
            this.apartment = apartment;
            return this;
        }

        public Builder setMsisdn(String msisdn) {
            this.msisdn = msisdn;
            return this;
        }

        public AgreementGenerationParams build() {
            return new AgreementGenerationParams(agreement, ats, str, building, apartment, msisdn);
        }
    }
}
