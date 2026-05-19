package com.pgmanager.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.Locale;

public class WhatsAppHelper {
    public static void sendWhatsAppMessage(Context context, String phone, String message) {
        try {
            // Remove non-numeric characters from phone
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (cleanPhone.length() == 10) {
                cleanPhone = "91" + cleanPhone; // Default to India
            }

            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getRentReminderMessage(String name, double amount, String pgName) {
        return String.format(Locale.ENGLISH, "Hi %s, your rent of ₹%.2f for %s is due. Please pay at the earliest. Thank you!", name, amount, pgName);
    }

    public static String getRentOverdueMessage(String name, double amount, String pgName) {
        return String.format(Locale.ENGLISH, "Hi %s, your rent of ₹%.2f for %s is *overdue*. Kindly clear the dues immediately. Thank you.", name, amount, pgName);
    }
}
