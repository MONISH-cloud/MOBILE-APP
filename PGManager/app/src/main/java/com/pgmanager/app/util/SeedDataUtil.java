package com.pgmanager.app.util;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SeedDataUtil {

    public interface OnSeedComplete {
        void onComplete();
        void onError(String error);
    }

    public static void seedDemoData(OnSeedComplete callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) { callback.onError("Not logged in"); return; }

        // We build everything in one WriteBatch so all data lands atomically.
        WriteBatch batch = db.batch();

        // ── helpers ──────────────────────────────────────────────────────────
        String col_pgs     = Constants.COLLECTION_PGS;
        String col_rooms   = Constants.COLLECTION_ROOMS;
        String col_beds    = Constants.COLLECTION_BEDS;
        String col_tenants = Constants.COLLECTION_TENANTS;
        String col_rent    = Constants.COLLECTION_RENT_RECORDS;

        // current + 2 previous months
        Calendar cal = Calendar.getInstance();
        String monthCur  = DateUtils.formatMonth(cal.getTime());
        cal.add(Calendar.MONTH, -1); String monthM1 = DateUtils.formatMonth(cal.getTime());
        cal.add(Calendar.MONTH, -1); String monthM2 = DateUtils.formatMonth(cal.getTime());

        // due date = 5th of current month
        Calendar dueCal = Calendar.getInstance();
        dueCal.set(Calendar.DAY_OF_MONTH, 5);
        dueCal.set(Calendar.HOUR_OF_DAY, 0); dueCal.set(Calendar.MINUTE, 0); dueCal.set(Calendar.SECOND, 0);
        Timestamp dueDateCur = new Timestamp(dueCal.getTime());

        // ═════════════════════════════════════════════════════════════════════
        // PG 1 — Sunrise PG  (3 rooms, 6 beds, 4 occupied)
        // ═════════════════════════════════════════════════════════════════════
        String pg1Id = db.collection(col_pgs).document().getId();
        String pg1Name = "Sunrise PG";
        putPG(batch, db, col_pgs, pg1Id, ownerId, pg1Name,
                "42, MG Road, Bangalore - 560001", 8000, 16000, 3, 6, 4, 12.9716, 77.5946);

        // rooms
        String r1A = newId(db, col_pgs, pg1Id, col_rooms);
        String r1B = newId(db, col_pgs, pg1Id, col_rooms);
        String r1C = newId(db, col_pgs, pg1Id, col_rooms);
        putRoom(batch, db, col_pgs, pg1Id, col_rooms, r1A, "101", 1, 2, 2);
        putRoom(batch, db, col_pgs, pg1Id, col_rooms, r1B, "102", 1, 2, 2);
        putRoom(batch, db, col_pgs, pg1Id, col_rooms, r1C, "201", 2, 2, 0);

        // beds
        String b1A1 = newBedId(db, col_pgs, pg1Id, col_rooms, r1A, col_beds);
        String b1A2 = newBedId(db, col_pgs, pg1Id, col_rooms, r1A, col_beds);
        String b1B1 = newBedId(db, col_pgs, pg1Id, col_rooms, r1B, col_beds);
        String b1B2 = newBedId(db, col_pgs, pg1Id, col_rooms, r1B, col_beds);
        String b1C1 = newBedId(db, col_pgs, pg1Id, col_rooms, r1C, col_beds);
        String b1C2 = newBedId(db, col_pgs, pg1Id, col_rooms, r1C, col_beds);

        // tenants for PG1
        String t1 = newId(db, col_tenants);
        String t2 = newId(db, col_tenants);
        String t3 = newId(db, col_tenants);
        String t4 = newId(db, col_tenants);

        // beds — occupied ones carry tenantId + tenantName
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1A, col_beds, b1A1, "101-A", true, t1, "Rahul Sharma");
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1A, col_beds, b1A2, "101-B", true, t2, "Priya Patel");
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1B, col_beds, b1B1, "102-A", true, t3, "Amit Kumar");
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1B, col_beds, b1B2, "102-B", true, t4, "Sneha Reddy");
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1C, col_beds, b1C1, "201-A", false, null, null);
        putBed(batch, db, col_pgs, pg1Id, col_rooms, r1C, col_beds, b1C2, "201-B", false, null, null);

        // tenants
        putTenant(batch, db, col_tenants, t1, pg1Id, pg1Name, r1A, "101", b1A1, "101-A",
                "Rahul Sharma", "9845012345", 8000, 16000, Constants.STATUS_ACTIVE, monthsAgo(3), null, null);
        putTenant(batch, db, col_tenants, t2, pg1Id, pg1Name, r1A, "101", b1A2, "101-B",
                "Priya Patel", "9741023456", 7500, 15000, Constants.STATUS_NOTICE, monthsAgo(5), daysAgo(10), daysFromNow(20));
        putTenant(batch, db, col_tenants, t3, pg1Id, pg1Name, r1B, "102", b1B1, "102-A",
                "Amit Kumar", "8123456789", 8500, 17000, Constants.STATUS_ACTIVE, monthsAgo(2), null, null);
        putTenant(batch, db, col_tenants, t4, pg1Id, pg1Name, r1B, "102", b1B2, "102-B",
                "Sneha Reddy", "9632587410", 8000, 16000, Constants.STATUS_ACTIVE, monthsAgo(1), null, null);

        // rent records — current month
        putRent(batch, db, col_rent, t1, pg1Id, pg1Name, "Rahul Sharma", "9845012345", 8000, monthCur, dueDateCur, Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t2, pg1Id, pg1Name, "Priya Patel",  "9741023456", 7500, monthCur, dueDateCur, Constants.STATUS_PENDING, false);
        putRent(batch, db, col_rent, t3, pg1Id, pg1Name, "Amit Kumar",   "8123456789", 8500, monthCur, dueDateCur, Constants.STATUS_OVERDUE, false);
        putRent(batch, db, col_rent, t4, pg1Id, pg1Name, "Sneha Reddy",  "9632587410", 8000, monthCur, dueDateCur, Constants.STATUS_PAID, true);

        // rent records — last month
        putRent(batch, db, col_rent, t1, pg1Id, pg1Name, "Rahul Sharma", "9845012345", 8000, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t2, pg1Id, pg1Name, "Priya Patel",  "9741023456", 7500, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t3, pg1Id, pg1Name, "Amit Kumar",   "8123456789", 8500, monthM1, pastDue(1), Constants.STATUS_PAID, true);

        // rent records — 2 months ago
        putRent(batch, db, col_rent, t1, pg1Id, pg1Name, "Rahul Sharma", "9845012345", 8000, monthM2, pastDue(2), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t2, pg1Id, pg1Name, "Priya Patel",  "9741023456", 7500, monthM2, pastDue(2), Constants.STATUS_PAID, true);

        // ═════════════════════════════════════════════════════════════════════
        // PG 2 — Green Valley PG  (3 rooms, 6 beds, 3 occupied)
        // ═════════════════════════════════════════════════════════════════════
        String pg2Id = db.collection(col_pgs).document().getId();
        String pg2Name = "Green Valley PG";
        putPG(batch, db, col_pgs, pg2Id, ownerId, pg2Name,
                "15, Koramangala 5th Block, Bangalore - 560095", 7500, 15000, 3, 6, 3, 12.9352, 77.6245);

        String r2A = newId(db, col_pgs, pg2Id, col_rooms);
        String r2B = newId(db, col_pgs, pg2Id, col_rooms);
        String r2C = newId(db, col_pgs, pg2Id, col_rooms);
        putRoom(batch, db, col_pgs, pg2Id, col_rooms, r2A, "101", 1, 2, 2);
        putRoom(batch, db, col_pgs, pg2Id, col_rooms, r2B, "102", 1, 2, 1);
        putRoom(batch, db, col_pgs, pg2Id, col_rooms, r2C, "201", 2, 2, 0);

        String b2A1 = newBedId(db, col_pgs, pg2Id, col_rooms, r2A, col_beds);
        String b2A2 = newBedId(db, col_pgs, pg2Id, col_rooms, r2A, col_beds);
        String b2B1 = newBedId(db, col_pgs, pg2Id, col_rooms, r2B, col_beds);
        String b2B2 = newBedId(db, col_pgs, pg2Id, col_rooms, r2B, col_beds);
        String b2C1 = newBedId(db, col_pgs, pg2Id, col_rooms, r2C, col_beds);
        String b2C2 = newBedId(db, col_pgs, pg2Id, col_rooms, r2C, col_beds);

        String t5 = newId(db, col_tenants);
        String t6 = newId(db, col_tenants);
        String t7 = newId(db, col_tenants);

        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2A, col_beds, b2A1, "101-A", true,  t5, "Vikram Singh");
        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2A, col_beds, b2A2, "101-B", true,  t6, "Anjali Mehta");
        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2B, col_beds, b2B1, "102-A", true,  t7, "Rohit Verma");
        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2B, col_beds, b2B2, "102-B", false, null, null);
        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2C, col_beds, b2C1, "201-A", false, null, null);
        putBed(batch, db, col_pgs, pg2Id, col_rooms, r2C, col_beds, b2C2, "201-B", false, null, null);

        putTenant(batch, db, col_tenants, t5, pg2Id, pg2Name, r2A, "101", b2A1, "101-A",
                "Vikram Singh", "7890123456", 7500, 15000, Constants.STATUS_ACTIVE, monthsAgo(4), null, null);
        putTenant(batch, db, col_tenants, t6, pg2Id, pg2Name, r2A, "101", b2A2, "101-B",
                "Anjali Mehta", "9087654321", 7000, 14000, Constants.STATUS_ACTIVE, monthsAgo(2), null, null);
        putTenant(batch, db, col_tenants, t7, pg2Id, pg2Name, r2B, "102", b2B1, "102-A",
                "Rohit Verma", "8765432109", 8000, 16000, Constants.STATUS_ACTIVE, monthsAgo(1), null, null);

        putRent(batch, db, col_rent, t5, pg2Id, pg2Name, "Vikram Singh", "7890123456", 7500, monthCur, dueDateCur, Constants.STATUS_OVERDUE, false);
        putRent(batch, db, col_rent, t6, pg2Id, pg2Name, "Anjali Mehta", "9087654321", 7000, monthCur, dueDateCur, Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t7, pg2Id, pg2Name, "Rohit Verma",  "8765432109", 8000, monthCur, dueDateCur, Constants.STATUS_PENDING, false);

        putRent(batch, db, col_rent, t5, pg2Id, pg2Name, "Vikram Singh", "7890123456", 7500, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t6, pg2Id, pg2Name, "Anjali Mehta", "9087654321", 7000, monthM1, pastDue(1), Constants.STATUS_PAID, true);

        putRent(batch, db, col_rent, t5, pg2Id, pg2Name, "Vikram Singh", "7890123456", 7500, monthM2, pastDue(2), Constants.STATUS_PAID, true);

        // ═════════════════════════════════════════════════════════════════════
        // PG 3 — Royal Residency  (3 rooms, 6 beds, 5 occupied)
        // ═════════════════════════════════════════════════════════════════════
        String pg3Id = db.collection(col_pgs).document().getId();
        String pg3Name = "Royal Residency";
        putPG(batch, db, col_pgs, pg3Id, ownerId, pg3Name,
                "88, Indiranagar 100ft Road, Bangalore - 560038", 9500, 19000, 3, 6, 5, 12.9784, 77.6408);

        String r3A = newId(db, col_pgs, pg3Id, col_rooms);
        String r3B = newId(db, col_pgs, pg3Id, col_rooms);
        String r3C = newId(db, col_pgs, pg3Id, col_rooms);
        putRoom(batch, db, col_pgs, pg3Id, col_rooms, r3A, "101", 1, 2, 2);
        putRoom(batch, db, col_pgs, pg3Id, col_rooms, r3B, "102", 1, 2, 2);
        putRoom(batch, db, col_pgs, pg3Id, col_rooms, r3C, "201", 2, 2, 1);

        String b3A1 = newBedId(db, col_pgs, pg3Id, col_rooms, r3A, col_beds);
        String b3A2 = newBedId(db, col_pgs, pg3Id, col_rooms, r3A, col_beds);
        String b3B1 = newBedId(db, col_pgs, pg3Id, col_rooms, r3B, col_beds);
        String b3B2 = newBedId(db, col_pgs, pg3Id, col_rooms, r3B, col_beds);
        String b3C1 = newBedId(db, col_pgs, pg3Id, col_rooms, r3C, col_beds);
        String b3C2 = newBedId(db, col_pgs, pg3Id, col_rooms, r3C, col_beds);

        String t8  = newId(db, col_tenants);
        String t9  = newId(db, col_tenants);
        String t10 = newId(db, col_tenants);
        String t11 = newId(db, col_tenants);
        String t12 = newId(db, col_tenants);

        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3A, col_beds, b3A1, "101-A", true,  t8,  "Pooja Nair");
        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3A, col_beds, b3A2, "101-B", true,  t9,  "Karan Joshi");
        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3B, col_beds, b3B1, "102-A", true,  t10, "Divya Iyer");
        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3B, col_beds, b3B2, "102-B", true,  t11, "Suresh Babu");
        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3C, col_beds, b3C1, "201-A", true,  t12, "Meera Pillai");
        putBed(batch, db, col_pgs, pg3Id, col_rooms, r3C, col_beds, b3C2, "201-B", false, null, null);

        putTenant(batch, db, col_tenants, t8,  pg3Id, pg3Name, r3A, "101", b3A1, "101-A",
                "Pooja Nair",   "9543210987", 9500, 19000, Constants.STATUS_ACTIVE, monthsAgo(6), null, null);
        putTenant(batch, db, col_tenants, t9,  pg3Id, pg3Name, r3A, "101", b3A2, "101-B",
                "Karan Joshi",  "7654321098", 9500, 19000, Constants.STATUS_ACTIVE, monthsAgo(3), null, null);
        putTenant(batch, db, col_tenants, t10, pg3Id, pg3Name, r3B, "102", b3B1, "102-A",
                "Divya Iyer",   "9321098765", 9000, 18000, Constants.STATUS_ACTIVE, monthsAgo(2), null, null);
        putTenant(batch, db, col_tenants, t11, pg3Id, pg3Name, r3B, "102", b3B2, "102-B",
                "Suresh Babu",  "8210987654", 9500, 19000, Constants.STATUS_NOTICE, monthsAgo(7), daysAgo(5), daysFromNow(25));
        putTenant(batch, db, col_tenants, t12, pg3Id, pg3Name, r3C, "201", b3C1, "201-A",
                "Meera Pillai", "9109876543", 9000, 18000, Constants.STATUS_ACTIVE, monthsAgo(1), null, null);

        putRent(batch, db, col_rent, t8,  pg3Id, pg3Name, "Pooja Nair",   "9543210987", 9500, monthCur, dueDateCur, Constants.STATUS_PAID,    true);
        putRent(batch, db, col_rent, t9,  pg3Id, pg3Name, "Karan Joshi",  "7654321098", 9500, monthCur, dueDateCur, Constants.STATUS_OVERDUE,  false);
        putRent(batch, db, col_rent, t10, pg3Id, pg3Name, "Divya Iyer",   "9321098765", 9000, monthCur, dueDateCur, Constants.STATUS_PENDING,  false);
        putRent(batch, db, col_rent, t11, pg3Id, pg3Name, "Suresh Babu",  "8210987654", 9500, monthCur, dueDateCur, Constants.STATUS_PENDING,  false);
        putRent(batch, db, col_rent, t12, pg3Id, pg3Name, "Meera Pillai", "9109876543", 9000, monthCur, dueDateCur, Constants.STATUS_PAID,     true);

        putRent(batch, db, col_rent, t8,  pg3Id, pg3Name, "Pooja Nair",   "9543210987", 9500, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t9,  pg3Id, pg3Name, "Karan Joshi",  "7654321098", 9500, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t10, pg3Id, pg3Name, "Divya Iyer",   "9321098765", 9000, monthM1, pastDue(1), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t11, pg3Id, pg3Name, "Suresh Babu",  "8210987654", 9500, monthM1, pastDue(1), Constants.STATUS_PAID, true);

        putRent(batch, db, col_rent, t8,  pg3Id, pg3Name, "Pooja Nair",   "9543210987", 9500, monthM2, pastDue(2), Constants.STATUS_PAID, true);
        putRent(batch, db, col_rent, t9,  pg3Id, pg3Name, "Karan Joshi",  "7654321098", 9500, monthM2, pastDue(2), Constants.STATUS_PAID, true);

        // ── commit everything ─────────────────────────────────────────────────
        batch.commit()
                .addOnSuccessListener(a -> callback.onComplete())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── write helpers ─────────────────────────────────────────────────────────

    private static void putPG(WriteBatch b, FirebaseFirestore db, String col, String pgId,
            String ownerId, String name, String address,
            double rent, double advance, int rooms, int beds, int occupied,
            double lat, double lng) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", pgId); m.put("ownerId", ownerId);
        m.put("managerIds", new ArrayList<>());
        m.put("name", name); m.put("address", address);
        m.put("defaultRent", rent); m.put("defaultAdvance", advance);
        m.put("totalRooms", rooms); m.put("totalBeds", beds); m.put("occupiedBeds", occupied);
        m.put("latitude", lat); m.put("longitude", lng);
        m.put("createdAt", Timestamp.now()); m.put("updatedAt", Timestamp.now());
        b.set(db.collection(col).document(pgId), m);
    }

    private static void putRoom(WriteBatch b, FirebaseFirestore db, String col, String pgId,
            String colR, String roomId, String num, int floor, int total, int occupied) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", roomId); m.put("roomNumber", num);
        m.put("floor", floor); m.put("totalBeds", total); m.put("occupiedBeds", occupied);
        b.set(db.collection(col).document(pgId).collection(colR).document(roomId), m);
    }

    private static void putBed(WriteBatch b, FirebaseFirestore db, String col, String pgId,
            String colR, String roomId, String colB, String bedId,
            String num, boolean occupied, String tenantId, String tenantName) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", bedId); m.put("bedNumber", num);
        m.put("status", occupied ? Constants.BED_OCCUPIED : Constants.BED_VACANT);
        if (tenantId != null)   m.put("tenantId", tenantId);
        if (tenantName != null) m.put("tenantName", tenantName);
        b.set(db.collection(col).document(pgId).collection(colR).document(roomId)
                .collection(colB).document(bedId), m);
    }

    private static void putTenant(WriteBatch b, FirebaseFirestore db, String col, String tenantId,
            String pgId, String pgName, String roomId, String roomNum,
            String bedId, String bedNum, String name, String phone,
            double rent, double advance, String status,
            Timestamp joined, Timestamp noticeDate, Timestamp moveOut) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", tenantId); m.put("pgId", pgId); m.put("pgName", pgName);
        m.put("roomId", roomId); m.put("roomNumber", roomNum);
        m.put("bedId", bedId); m.put("bedNumber", bedNum);
        m.put("name", name); m.put("whatsappNumber", phone);
        m.put("rentAmount", rent); m.put("advanceAmount", advance);
        m.put("status", status); m.put("joiningDate", joined);
        m.put("createdAt", Timestamp.now());
        if (noticeDate != null) m.put("noticeDate", noticeDate);
        if (moveOut != null)    m.put("expectedMoveOutDate", moveOut);
        b.set(db.collection(col).document(tenantId), m);
    }

    private static void putRent(WriteBatch b, FirebaseFirestore db, String col,
            String tenantId, String pgId, String pgName,
            String tenantName, String phone, double amount,
            String month, Timestamp dueDate, String status, boolean paid) {
        String rentId = tenantId + "_" + month;
        Map<String, Object> m = new HashMap<>();
        m.put("id", rentId); m.put("tenantId", tenantId);
        m.put("pgId", pgId); m.put("pgName", pgName);
        m.put("tenantName", tenantName); m.put("whatsappNumber", phone);
        m.put("month", month); m.put("amount", amount);
        m.put("dueDate", dueDate); m.put("status", status);
        m.put("reminderSent", false); m.put("rewardApplied", 0.0);
        m.put("createdAt", Timestamp.now());
        if (paid) m.put("paidDate", Timestamp.now());
        b.set(db.collection(col).document(rentId), m);
    }

    // ── ID generators ─────────────────────────────────────────────────────────

    private static String newId(FirebaseFirestore db, String col) {
        return db.collection(col).document().getId();
    }

    private static String newId(FirebaseFirestore db, String col, String docId, String sub) {
        return db.collection(col).document(docId).collection(sub).document().getId();
    }

    private static String newBedId(FirebaseFirestore db, String col, String pgId,
            String colR, String roomId, String colB) {
        return db.collection(col).document(pgId).collection(colR)
                .document(roomId).collection(colB).document().getId();
    }

    // ── date helpers ──────────────────────────────────────────────────────────

    private static Timestamp monthsAgo(int n) {
        Calendar c = Calendar.getInstance(); c.add(Calendar.MONTH, -n); return new Timestamp(c.getTime());
    }

    private static Timestamp daysAgo(int n) {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -n); return new Timestamp(c.getTime());
    }

    private static Timestamp daysFromNow(int n) {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, n); return new Timestamp(c.getTime());
    }

    private static Timestamp pastDue(int monthsBack) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -monthsBack);
        c.set(Calendar.DAY_OF_MONTH, 5);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
        return new Timestamp(c.getTime());
    }
}
