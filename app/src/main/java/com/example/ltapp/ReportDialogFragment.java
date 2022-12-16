package com.example.ltapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ReportDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        final EditText input = new EditText(requireContext());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );

        input.setLayoutParams(lp);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Report a erroneous translation")
                .setMessage("Please enter the correct translation, and we will look into correcting it. Sorry for the inconvenience")
                .setView(input)
                .setPositiveButton("OK", (dialogInteface, i) -> dismiss())
                .setNegativeButton("Cancel", (dialogInterface, i) -> dismiss());

        return builder.create();
    }
}
