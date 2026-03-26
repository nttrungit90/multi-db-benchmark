package com.benchmark.core;

import java.util.List;

public interface DatabaseDriver {

    String name();

    void insertBatch(List<HotelRoomPrice> records);

    void upsertBatch(List<HotelRoomPrice> records);

    List<HotelRoomPrice> readByHotelId(long hotelId);

    void cleanup();

    long getStorageSizeBytes();

    SchemaInfo getSchemaInfo();
}
