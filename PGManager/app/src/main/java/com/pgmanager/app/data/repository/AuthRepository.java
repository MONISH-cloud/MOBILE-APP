package com.pgmanager.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pgmanager.app.data.model.UserProfile;
import com.pgmanager.app.util.Constants;

public class AuthRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void logout() {
        auth.signOut();
    }

    public Task<UserProfile> getUserProfile(String uid) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .continueWith(task -> task.getResult().toObject(UserProfile.class));
    }

    public Task<Void> saveUserProfile(UserProfile profile) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(profile.getUserId())
                .set(profile);
    }
}
