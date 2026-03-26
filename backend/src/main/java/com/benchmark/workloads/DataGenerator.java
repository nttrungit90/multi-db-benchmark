package com.benchmark.workloads;

import com.benchmark.core.HotelRoomPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DataGenerator {

    private static final String[] SUPPLIER_CODES = {
            "agoda", "booking", "expedia", "hotels", "trip", "kayak", "trivago", "priceline"
    };
    private static final String[] SUPPLIER_CHANNELS = {
            "direct", "api", "scraper", "affiliate", "meta"
    };

    public static List<HotelRoomPrice> generate(long startIndex, int count) {
        List<HotelRoomPrice> records = new ArrayList<>(count);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (long i = startIndex; i < startIndex + count; i++) {
            long hotelId = (i / 100) + 1;
            long roomTypeId = (i % 10) + 1;
            String supplierCode = SUPPLIER_CODES[(int) (i % SUPPLIER_CODES.length)];
            String supplierChannel = SUPPLIER_CHANNELS[(int) ((i / SUPPLIER_CODES.length) % SUPPLIER_CHANNELS.length)];
            LocalDate checkin = LocalDate.of(2026, 1, 1).plusDays(i % 365);
            BigDecimal price = BigDecimal.valueOf(50 + rng.nextDouble() * 950).setScale(2, RoundingMode.HALF_UP);

            records.add(new HotelRoomPrice(
                    hotelId, roomTypeId, supplierCode, supplierChannel,
                    checkin, price, LocalDateTime.now()
            ));
        }
        return records;
    }

    public static List<HotelRoomPrice> generateForUpsert(long startIndex, int count) {
        List<HotelRoomPrice> records = generate(startIndex, count);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<HotelRoomPrice> updated = new ArrayList<>(count);
        for (HotelRoomPrice r : records) {
            BigDecimal newPrice = BigDecimal.valueOf(50 + rng.nextDouble() * 950).setScale(2, RoundingMode.HALF_UP);
            updated.add(new HotelRoomPrice(
                    r.hotelId(), r.wegoRoomTypeId(), r.supplierCode(), r.supplierChannel(),
                    r.checkinDate(), newPrice, LocalDateTime.now()
            ));
        }
        return updated;
    }
}
