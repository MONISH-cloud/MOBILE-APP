package com.pgmanager.app.util;

public class Constants {
    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PGS = "pgs";
    public static final String COLLECTION_ROOMS = "rooms";
    public static final String COLLECTION_BEDS = "beds";
    public static final String COLLECTION_TENANTS = "tenants";
    public static final String COLLECTION_RENT_RECORDS = "rentRecords";
    public static final String COLLECTION_EXPENSES = "expenses";

    // Intent Extras
    public static final String EXTRA_PG_ID = "extra_pg_id";
    public static final String EXTRA_PG_NAME = "extra_pg_name";
    public static final String EXTRA_TENANT_ID = "extra_tenant_id";
    public static final String EXTRA_ROOM_ID = "extra_room_id";

    // Roles
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_TENANT = "tenant";

    // Tenant Status
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_NOTICE = "notice";
    public static final String STATUS_MOVED_OUT = "moved_out";

    // Rent Status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PAID = "paid";
    public static final String STATUS_OVERDUE = "overdue";

    // Bed Status
    public static final String BED_VACANT = "vacant";
    public static final String BED_OCCUPIED = "occupied";
}
