package com.example.dasproyecto.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Calendar;

public class ElegirFechaDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String ARG_DAY = "day";
    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";

    public static ElegirFechaDialog newInstance(int day, int month, int year) {
        ElegirFechaDialog frag = new ElegirFechaDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY, day);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int y, m, d;
        if (getArguments() != null && getArguments().containsKey(ARG_YEAR)) {
            y = getArguments().getInt(ARG_YEAR);
            m = getArguments().getInt(ARG_MONTH);
            d = getArguments().getInt(ARG_DAY);
        } else {
            final Calendar c = Calendar.getInstance();
            y = c.get(Calendar.YEAR);
            m = c.get(Calendar.MONTH);
            d = c.get(Calendar.DAY_OF_MONTH);
        }
        return new DatePickerDialog(getActivity(), this, y, m, d);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Bundle result = new Bundle();
        result.putInt("year", year);
        result.putInt("month", month);
        result.putInt("day", dayOfMonth);
        getParentFragmentManager().setFragmentResult("fechaSeleccionada", result);
    }
}
