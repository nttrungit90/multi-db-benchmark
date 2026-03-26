package com.benchmark.drivers;

import com.benchmark.core.DatabaseDriver;
import com.benchmark.core.HotelRoomPrice;
import com.benchmark.core.SchemaInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlDriver implements DatabaseDriver {

    private final JdbcTemplate jdbc;
    private final String dataDir;

    public MySqlDriver(JdbcTemplate jdbc, @Value("${benchmark.data-dir}") String dataDir) {
        this.jdbc = jdbc;
        this.dataDir = dataDir;
    }

    @Override
    public String name() {
        return "mysql";
    }

    @Override
    public void insertBatch(List<HotelRoomPrice> records) {
        String sql = "INSERT INTO hotel_room_latest_price " +
                "(hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date, final_price, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>(records.size());
        for (HotelRoomPrice r : records) {
            batchArgs.add(new Object[]{
                    r.hotelId(), r.wegoRoomTypeId(), r.supplierCode(), r.supplierChannel(),
                    Date.valueOf(r.checkinDate()), r.finalPrice(), Timestamp.valueOf(r.updatedAt())
            });
        }
        jdbc.batchUpdate(sql, batchArgs);
    }

    @Override
    public void upsertBatch(List<HotelRoomPrice> records) {
        String sql = "INSERT INTO hotel_room_latest_price " +
                "(hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date, final_price, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE final_price = VALUES(final_price), updated_at = VALUES(updated_at)";

        List<Object[]> batchArgs = new ArrayList<>(records.size());
        for (HotelRoomPrice r : records) {
            batchArgs.add(new Object[]{
                    r.hotelId(), r.wegoRoomTypeId(), r.supplierCode(), r.supplierChannel(),
                    Date.valueOf(r.checkinDate()), r.finalPrice(), Timestamp.valueOf(r.updatedAt())
            });
        }
        jdbc.batchUpdate(sql, batchArgs);
    }

    @Override
    public List<HotelRoomPrice> readByHotelId(long hotelId) {
        return jdbc.query(
                "SELECT * FROM hotel_room_latest_price WHERE hotel_id = ?",
                (rs, rowNum) -> new HotelRoomPrice(
                        rs.getLong("hotel_id"),
                        rs.getLong("wego_room_type_id"),
                        rs.getString("supplier_code"),
                        rs.getString("supplier_channel"),
                        rs.getDate("checkin_date").toLocalDate(),
                        rs.getBigDecimal("final_price"),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ),
                hotelId
        );
    }

    @Override
    public void cleanup() {
        jdbc.execute("TRUNCATE TABLE hotel_room_latest_price");
    }

    @Override
    public long getStorageSizeBytes() {
        File dir = new File(dataDir + "/mysql");
        return dirSize(dir);
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return new SchemaInfo(
                "mysql",
                """
                CREATE TABLE hotel_room_latest_price (
                    hotel_id BIGINT NOT NULL,
                    wego_room_type_id BIGINT NOT NULL,
                    supplier_code VARCHAR(50) NOT NULL,
                    supplier_channel VARCHAR(50) NOT NULL,
                    checkin_date DATE NOT NULL,
                    final_price DECIMAL(12, 2) NOT NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (hotel_id, wego_room_type_id,
                        supplier_code, supplier_channel, checkin_date)
                ) ENGINE=InnoDB;""",
                List.of("PRIMARY KEY (hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date)"),
                """
                INSERT INTO hotel_room_latest_price
                    (hotel_id, wego_room_type_id, supplier_code,
                     supplier_channel, checkin_date, final_price, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                """
                INSERT INTO hotel_room_latest_price
                    (hotel_id, wego_room_type_id, supplier_code,
                     supplier_channel, checkin_date, final_price, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    final_price = VALUES(final_price),
                    updated_at = VALUES(updated_at)""",
                "SELECT * FROM hotel_room_latest_price WHERE hotel_id = ?",
                "(hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date)"
        );
    }

    private long dirSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    size += f.isDirectory() ? dirSize(f) : f.length();
                }
            }
        }
        return size;
    }
}
