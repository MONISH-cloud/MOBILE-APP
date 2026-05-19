package com.pgmanager.app.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pgmanager.app.data.model.Bed;
import com.pgmanager.app.data.model.PG;
import com.pgmanager.app.data.model.Room;
import com.pgmanager.app.util.Constants;

public class PGRepository {
    private final FirebaseFirestore db;

    public PGRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public CollectionReference getPGCollection() {
        return db.collection(Constants.COLLECTION_PGS);
    }

    public Query getMyPGs(String userId) {
        return getPGCollection().whereEqualTo("ownerId", userId).orderBy("name");
    }

    public CollectionReference getRoomCollection(String pgId) {
        return getPGCollection().document(pgId).collection(Constants.COLLECTION_ROOMS);
    }

    public CollectionReference getBedCollection(String pgId, String roomId) {
        return getRoomCollection(pgId).document(roomId).collection(Constants.COLLECTION_BEDS);
    }

    public void updateBedStatus(String pgId, String roomId, String bedId, String status, String tenantId) {
        getBedCollection(pgId, roomId).document(bedId)
                .update("status", status, "tenantId", tenantId);
    }
}
