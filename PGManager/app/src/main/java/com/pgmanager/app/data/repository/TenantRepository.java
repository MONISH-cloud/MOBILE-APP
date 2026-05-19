package com.pgmanager.app.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pgmanager.app.util.Constants;

public class TenantRepository {
    private final FirebaseFirestore db;

    public TenantRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public CollectionReference getTenantCollection() {
        return db.collection(Constants.COLLECTION_TENANTS);
    }

    public Query getAllActiveTenants(String pgId) {
        // Use simple equality filter to avoid composite index requirement
        return getTenantCollection()
                .whereEqualTo("pgId", pgId)
                .whereEqualTo("status", Constants.STATUS_ACTIVE);
    }

    public Query getTenantsOnNotice() {
        return getTenantCollection()
                .whereEqualTo("status", Constants.STATUS_NOTICE)
                .orderBy("noticeDate");
    }
}
