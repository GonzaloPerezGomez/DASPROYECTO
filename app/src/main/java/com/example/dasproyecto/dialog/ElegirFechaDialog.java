package com.example.dasproyecto.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Calendar;

public class ElegirFechaDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private int dia, mes, anio = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
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
