package com.jaravir.tekila.engines;

import java.util.Date;

public class SipCharge {
    public final String sipNumber;
    public final String calledNumber;
    public final Date callTime;
    public final Integer duration; //seconds
    public final Double chargeAmount;

    public SipCharge(final String sipNumber,
              final String calledNumber,
              final Date callTime,
              final Integer duration,
              final Double chargeAmount) {
        this.sipNumber = sipNumber;
        this.calledNumber = calledNumber;
        this.callTime = callTime;
        this.duration = duration;
        this.chargeAmount = chargeAmount;
    }

    @Override
    public String toString() {
        return "SipCharge{" +
                "sipNumber='" + sipNumber + '\'' +
                ", calledNumber='" + calledNumber + '\'' +
                ", callTime=" + callTime +
                ", duration=" + duration +
                ", chargeAmount=" + chargeAmount +
                '}';
    }
}
