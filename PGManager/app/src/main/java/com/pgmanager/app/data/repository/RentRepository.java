package com.pgmanager.app.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pgmanager.app.util.Constants;

public class RentRepository {
    private final FirebaseFirestore db;

    public RentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public CollectionReference getRentCollection() {
        return db.collection(Constants.COLLECTION_RENT_RECORDS);
    }

    public Query getPendingRentForPG(String pgId) {
        return getRentCollection()
                .whereEqualTo("pgId", pgId)
                .whereEqualTo("status", Constants.STATUS_PENDING)
                .orderBy("dueDate");
    }

    public Query getRentHistoryForTenant(String tenantId) {
        return getRentCollection()
                .whereEqualTo("tenantId", tenantId)
                .orderBy("month", Query.Direction.DESCENDING);
    }

    public Query getOverdueRent() {
        return getRentCollection()
                .whereEqualTo("status", Constants.STATUS_OVERDUE);
    }
}
