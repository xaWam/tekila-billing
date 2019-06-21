package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.joda.time.DateTime;

/**
 * Created by kmaharov on 18.05.2017.
 */
public class VASCreationParams {
    public final Subscription subscription;
    public final ValueAddedService vas;
    public final IpAddress ipAddress;
    public final DateTime startDate;
    public final DateTime expiresDate;
    public final double count;
    public final long vasFee;
    public final String sipNumber;

    private VASCreationParams(
            Subscription subscription,
            ValueAddedService vas,
            IpAddress ipAddress,
            DateTime startDate,
            DateTime expiresDate,
            double count,
            long vasFee,
            String sipNumber
    ) {
        this.subscription = subscription;
        this.vas = vas;
        this.ipAddress = ipAddress;
        this.startDate = startDate;
        this.expiresDate = expiresDate;
        this.count = count;
        this.vasFee = vasFee;
        this.sipNumber = sipNumber;
    }

    public static class Builder {
        private Subscription subscription;
        private ValueAddedService vas;
        private IpAddress ipAddress;
        private DateTime startDate;
        private DateTime expiresDate;
        private double count;
        private long vasFee;
        private String sipNumber;

        public Builder() {}


            public Builder setVasFee(long vasFee) {
                this.vasFee = vasFee;
                return this;
            }

            public Builder setSubscription(Subscription subscription) {
                this.subscription = subscription;
                return this;
            }

            public Builder setValueAddedService(ValueAddedService vas) {
                this.vas = vas;
                return this;
            }

            public Builder setIpAddress(IpAddress ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }

            public Builder setStartDate(DateTime dateTime) {
                this.startDate = dateTime;
                return this;
            }

            public Builder setExpiresDate(DateTime dateTime) {
                this.expiresDate = dateTime;
                return this;
            }

            public Builder setCount(double count) {
            this.count = count;
            return this;
        }

        public Builder setSipNumber(String sipNumber) {
            this.sipNumber = sipNumber;
            return this;
        }

        public VASCreationParams build() {
            return new VASCreationParams(subscription, vas, ipAddress, startDate, expiresDate, count, vasFee, sipNumber);
        }

    }
}
