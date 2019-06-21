package com.jaravir.tekila.base.persistence.manager;

import com.jaravir.tekila.base.entity.BaseEntity;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

@Startup
@Singleton
public class Register {
    private Locale locale;

    public Locale getDefaultLocale() {
        return this.locale;
    }

    public DateTime getDefaultExpirationDate() {
        return DateTime.parse("31-12-9999 23:59:59", DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public DateTime getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return new DateTime(calendar.getTime());
    }

    public DateTime getDateTimeWithTime(DateTime dateTime) {
        Calendar calendar = Calendar.getInstance();
        int year = dateTime.getYear();
        int month = dateTime.getMonthOfYear();
        int day = dateTime.getDayOfMonth();
        calendar.set(year, month, day, 23, 59, 59);
        return new DateTime(calendar.getTime());
    }

    public DateTime makeDateTime(int year, int month, int day){
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 23, 59, 59);
        return new DateTime(calendar.getTime());
    }
}
