package com.pgmanager.app.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.pgmanager.app.R;
import com.pgmanager.app.data.model.Expense;
import com.pgmanager.app.data.repository.ExpenseRepository;
import com.pgmanager.app.databinding.ActivityAddExpenseBinding;
import com.pgmanager.app.util.Constants;
import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {
    private ActivityAddExpenseBinding binding;
    private ExpenseRepository expenseRepo;
    private String pgId;
    private java.util.Calendar selectedDate = java.util.Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expenseRepo = new ExpenseRepository();
        pgId = getIntent().getStringExtra(Constants.EXTRA_PG_ID);

        if (pgId == null || pgId.isEmpty()) {
            Toast.makeText(this, "No PG selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupCategorySpinner();
        setupDatePicker();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveExpense());
    }

    private void setupCategorySpinner() {
        String[] categories = new String[]{
                getString(R.string.cat_vegetables),
                getString(R.string.cat_repairs),
                getString(R.string.cat_electricity),
                getString(R.string.cat_water),
                getString(R.string.cat_cleaning),
                getString(R.string.cat_salary),
                getString(R.string.cat_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        binding.etDate.setText(selectedDate.get(Calendar.DAY_OF_MONTH) + "/" + (selectedDate.get(Calendar.MONTH) + 1) + "/" + selectedDate.get(Calendar.YEAR));
        binding.etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                binding.etDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void saveExpense() {
        String category = binding.spinnerCategory.getText().toString().trim();
        String amountStr = binding.etAmount.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        if (category.isEmpty()) {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense expense = new Expense();
        expense.setId(expenseRepo.getExpenseCollection(pgId).document().getId());
        expense.setPgId(pgId);
        expense.setCategory(category);
        expense.setAmount(Double.parseDouble(amountStr));
        expense.setDescription(desc);
        expense.setDate(new Timestamp(selectedDate.getTime()));
        expense.setCreatedAt(Timestamp.now());

        expenseRepo.getExpenseCollection(pgId).document(expense.getId()).set(expense).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Expense recorded", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
