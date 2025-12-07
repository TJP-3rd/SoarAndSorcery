package com.example.soarandsorcery;

import android.app.Dialog;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class GameOverDialog extends DialogFragment {

    private final Runnable onContinue;

    public GameOverDialog(Runnable onContinue) {
        this.onContinue = onContinue;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("Game Over")
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (onContinue != null) {
                        onContinue.run();
                    }
                })
                .create();
    }
}
