package com.benchmark.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HotelRoomPrice(
        long hotelId,
        long wegoRoomTypeId,
        String supplierCode,
        String supplierChannel,
        LocalDate checkinDate,
        BigDecimal finalPrice,
        LocalDateTime updatedAt
) {}
