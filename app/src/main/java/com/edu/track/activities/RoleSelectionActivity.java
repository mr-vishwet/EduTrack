package com.edu.track.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.edu.track.R;
import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        setupCardClick(R.id.card_admin, "ADMIN");
        setupCardClick(R.id.card_teacher, "TEACHER");
        setupCardClick(R.id.card_parent, "PARENT");
    }

    private void setupCardClick(int cardId, String role) {
        MaterialCardView card = findViewById(cardId);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("ROLE", role);
            startActivity(intent);
        });
    }
}
