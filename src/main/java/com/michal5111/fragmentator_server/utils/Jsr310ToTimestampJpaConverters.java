package com.michal5111.fragmentator_server.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Time;
import java.time.LocalTime;

public class Jsr310ToTimestampJpaConverters {

    private Jsr310ToTimestampJpaConverters() {
    }

    @Converter(autoApply = true)
    public static class InstantToTimestampConverter implements AttributeConverter<LocalTime, Time> {
        @Override
        public Time convertToDatabaseColumn(LocalTime localTime) {
            if (localTime == null) {
                return null;
            }
            long epochMilli = localTime.atDate(java.time.LocalDate.EPOCH)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            return new Time(epochMilli);
        }

        @Override
        public LocalTime convertToEntityAttribute(Time ts) {
            if (ts == null) {
                return null;
            }
            return ts.toLocalTime();
        }
    }
}
