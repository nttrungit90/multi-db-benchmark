package com.benchmark.drivers;

import com.benchmark.core.DatabaseDriver;
import com.benchmark.core.HotelRoomPrice;
import com.benchmark.core.SchemaInfo;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class MongoDriver implements DatabaseDriver {

    private static final String COLLECTION = "hotel_room_latest_price";
    private final MongoTemplate mongo;
    private final String dataDir;

    public MongoDriver(MongoTemplate mongo, @Value("${benchmark.data-dir}") String dataDir) {
        this.mongo = mongo;
        this.dataDir = dataDir;
        ensureIndexes();
    }

    private void ensureIndexes() {
        var collection = mongo.getCollection(COLLECTION);
        collection.createIndex(
                Indexes.ascending("hotel_id", "wego_room_type_id", "supplier_code", "supplier_channel", "checkin_date"),
                new IndexOptions().unique(true)
        );
        collection.createIndex(Indexes.ascending("hotel_id"));
    }

    @Override
    public String name() {
        return "mongodb";
    }

    @Override
    public void insertBatch(List<HotelRoomPrice> records) {
        var collection = mongo.getCollection(COLLECTION);
        List<Document> docs = new ArrayList<>(records.size());
        for (HotelRoomPrice r : records) {
            docs.add(toDocument(r));
        }
        collection.insertMany(docs, new InsertManyOptions().ordered(false));
    }

    @Override
    public void upsertBatch(List<HotelRoomPrice> records) {
        var collection = mongo.getCollection(COLLECTION);
        List<WriteModel<Document>> ops = new ArrayList<>(records.size());
        for (HotelRoomPrice r : records) {
            Document filter = new Document()
                    .append("hotel_id", r.hotelId())
                    .append("wego_room_type_id", r.wegoRoomTypeId())
                    .append("supplier_code", r.supplierCode())
                    .append("supplier_channel", r.supplierChannel())
                    .append("checkin_date", r.checkinDate().toString());

            Document update = new Document("$set", new Document()
                    .append("final_price", r.finalPrice().doubleValue())
                    .append("updated_at", Date.from(r.updatedAt().atZone(ZoneId.systemDefault()).toInstant())));

            // Also set on insert
            Document setOnInsert = new Document("$setOnInsert", filter);

            ops.add(new UpdateOneModel<>(filter,
                    new Document().append("$set", new Document()
                            .append("final_price", r.finalPrice().doubleValue())
                            .append("updated_at", Date.from(r.updatedAt().atZone(ZoneId.systemDefault()).toInstant())))
                            .append("$setOnInsert", filter),
                    new UpdateOptions().upsert(true)));
        }
        collection.bulkWrite(ops, new BulkWriteOptions().ordered(false));
    }

    @Override
    public List<HotelRoomPrice> readByHotelId(long hotelId) {
        var collection = mongo.getCollection(COLLECTION);
        List<HotelRoomPrice> results = new ArrayList<>();
        for (Document doc : collection.find(new Document("hotel_id", hotelId))) {
            results.add(fromDocument(doc));
        }
        return results;
    }

    @Override
    public void cleanup() {
        mongo.dropCollection(COLLECTION);
        ensureIndexes();
    }

    @Override
    public long getStorageSizeBytes() {
        File dir = new File(dataDir + "/mongodb");
        return dirSize(dir);
    }

    @Override
    public SchemaInfo getSchemaInfo() {
        return new SchemaInfo(
                "mongodb",
                """
                Collection: hotel_room_latest_price
                Document structure:
                {
                    hotel_id: Long,
                    wego_room_type_id: Long,
                    supplier_code: String,
                    supplier_channel: String,
                    checkin_date: String (ISO date),
                    final_price: Double,
                    updated_at: Date
                }""",
                List.of(
                        "Unique compound index: { hotel_id: 1, wego_room_type_id: 1, supplier_code: 1, supplier_channel: 1, checkin_date: 1 }",
                        "Secondary index: { hotel_id: 1 }"
                ),
                """
                db.hotel_room_latest_price.insertMany([
                    { hotel_id, wego_room_type_id, supplier_code,
                      supplier_channel, checkin_date, final_price,
                      updated_at }
                ], { ordered: false })""",
                """
                db.hotel_room_latest_price.bulkWrite([
                    { updateOne: {
                        filter: { hotel_id, wego_room_type_id,
                                  supplier_code, supplier_channel,
                                  checkin_date },
                        update: { $set: { final_price, updated_at },
                                  $setOnInsert: { <filter fields> } },
                        upsert: true
                    }}
                ], { ordered: false })""",
                "db.hotel_room_latest_price.find({ hotel_id: <value> })",
                "Compound unique: (hotel_id, wego_room_type_id, supplier_code, supplier_channel, checkin_date)"
        );
    }

    private Document toDocument(HotelRoomPrice r) {
        return new Document()
                .append("hotel_id", r.hotelId())
                .append("wego_room_type_id", r.wegoRoomTypeId())
                .append("supplier_code", r.supplierCode())
                .append("supplier_channel", r.supplierChannel())
                .append("checkin_date", r.checkinDate().toString())
                .append("final_price", r.finalPrice().doubleValue())
                .append("updated_at", Date.from(r.updatedAt().atZone(ZoneId.systemDefault()).toInstant()));
    }

    private HotelRoomPrice fromDocument(Document doc) {
        return new HotelRoomPrice(
                doc.getLong("hotel_id"),
                doc.getLong("wego_room_type_id"),
                doc.getString("supplier_code"),
                doc.getString("supplier_channel"),
                LocalDate.parse(doc.getString("checkin_date")),
                BigDecimal.valueOf(doc.getDouble("final_price")),
                LocalDateTime.ofInstant(doc.getDate("updated_at").toInstant(), ZoneId.systemDefault())
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
