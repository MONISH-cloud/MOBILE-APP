package com.pgmanager.app.data.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pgmanager.app.util.Constants;

public class ExpenseRepository {
    private final FirebaseFirestore db;

    public ExpenseRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public CollectionReference getExpenseCollection(String pgId) {
        return db.collection(Constants.COLLECTION_PGS).document(pgId).collection(Constants.COLLECTION_EXPENSES);
    }

    public Query getMonthlyExpenses(String pgId) {
        return getExpenseCollection(pgId).orderBy("date", Query.Direction.DESCENDING);
    }
}
