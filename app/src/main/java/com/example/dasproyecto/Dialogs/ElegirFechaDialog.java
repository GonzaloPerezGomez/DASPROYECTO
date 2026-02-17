package com.example.dasproyecto.Dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Calendar;

public class ElegirFechaDialog extends DialogFragment {

    private DatePickerDialog.OnDateSetListener listener;
    private int dia, mes, anio = 0;

    public static ElegirFechaDialog newInstance(DatePickerDialog.OnDateSetListener listener) {
        ElegirFechaDialog fragment = new ElegirFechaDialog();
        fragment.listener = listener;
        return fragment;
    }

    public static ElegirFechaDialog newInstance(int d, int m, int a, DatePickerDialog.OnDateSetListener listener) {
        ElegirFechaDialog fragment = new ElegirFechaDialog();
        fragment.dia = d;
        fragment.mes = m;
        fragment.anio = a;
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (anio == 0) {
            final Calendar c = Calendar.getInstance();
            anio = c.get(Calendar.YEAR);
            mes = c.get(Calendar.MONTH);
            dia = c.get(Calendar.DAY_OF_MONTH);
        }
        return new DatePickerDialog(getActivity(), listener, anio, mes, dia);
    }
}
