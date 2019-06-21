package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import org.joda.time.DateTime;

/**
 * Created by KamranMa on 11.01.2018.
 */
public class VasEditParams {
    public final Subscription subscription;
    public final long sbnVasId;
    public final IpAddress ipAddress;
    public final DateTime expiresDate;
    public final int count;
    public final String sipNumber;

    private VasEditParams(
            Subscription subscription,
            long sbnVasId,
            IpAddress ipAddress,
            DateTime expiresDate,
            int count,
            String sipNumber
    ) {
        this.subscription = subscription;
        this.sbnVasId = sbnVasId;
        this.ipAddress = ipAddress;
        this.expiresDate = expiresDate;
        this.count = count;
        this.sipNumber = sipNumber;
    }

    public static class Builder {
        private Subscription subscription;
        private long sbnVasId;
        private IpAddress ipAddress;
        private DateTime expiresDate;
        private int count;
        private String sipNumber;

        public Builder() {}

        public Builder setSubscription(Subscription subscription) {
            this.subscription = subscription;
            return this;
        }

        public Builder setSubscriptionVasId(long vasId) {
            this.sbnVasId = vasId;
            return this;
        }

        public Builder setIpAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder setExpiresDate(DateTime dateTime) {
            this.expiresDate = dateTime;
            return this;
        }

        public Builder setCount(int count) {
            this.count = count;
            return this;
        }

        public Builder setSipNumber(String sipNumber) {
            this.sipNumber = sipNumber;
            return this;
        }

        public VasEditParams build() {
            return new VasEditParams(subscription, sbnVasId, ipAddress, expiresDate, count, sipNumber);
        }

    }
}
