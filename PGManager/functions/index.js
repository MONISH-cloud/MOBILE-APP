const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

/**
 * Scheduled Function: Generate Monthly Rent
 * Runs on the 1st of every month at 00:00
 */
exports.generateMonthlyRent = functions.pubsub.schedule('0 0 1 * *')
    .onRun(async (context) => {
        const tenantsSnapshot = await db.collection('tenants')
            .where('status', 'in', ['active', 'notice'])
            .get();

        const month = new Date().toISOString().slice(0, 7); // e.g., "2026-04"
        const batch = db.batch();
        
        // Set due date to 5th of current month
        const dueDateObj = new Date();
        dueDateObj.setDate(5);
        const dueDate = admin.firestore.Timestamp.fromDate(dueDateObj);

        tenantsSnapshot.forEach(doc => {
            const tenant = doc.data();
            const recordId = `${tenant.id}_${month}`;
            
            const rentRecord = {
                id: recordId,
                tenantId: tenant.id,
                pgId: tenant.pgId,
                tenantName: tenant.name,
                pgName: tenant.pgName,
                whatsappNumber: tenant.whatsappNumber,
                month: month,
                amount: tenant.rentAmount,
                dueDate: dueDate,
                status: 'pending',
                reminderSent: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            };

            batch.set(db.collection('rentRecords').doc(recordId), rentRecord);
        });

        await batch.commit();
        console.log(`Generated bills for ${tenantsSnapshot.size} tenants for ${month}`);
        return null;
    });

/**
 * Scheduled Function: Mark Overdue Rent
 * Runs daily at midnight
 */
exports.checkOverdueRent = functions.pubsub.schedule('0 0 * * *')
    .onRun(async (context) => {
        const today = admin.firestore.Timestamp.now();
        const pendingSnapshot = await db.collection('rentRecords')
            .where('status', '==', 'pending')
            .where('dueDate', '<', today)
            .get();

        const batch = db.batch();
        pendingSnapshot.forEach(doc => {
            batch.update(doc.ref, { status: 'overdue' });
        });

        await batch.commit();
        console.log(`Marked ${pendingSnapshot.size} records as overdue`);
        return null;
    });

/**
 * WhatsApp Webhook (Placeholder for Phase 2)
 * Handles incoming messages from Meta API
 */
exports.whatsappWebhook = functions.https.onRequest(async (req, res) => {
    // Phase 2 implementation for automated bot replies and collection
    res.status(200).send('EVENT_RECEIVED');
});
