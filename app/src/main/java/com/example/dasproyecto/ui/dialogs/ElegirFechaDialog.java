package com.example.dasproyecto.ui.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Calendar;

/**
 * Diálogo que muestra el selector de fecha nativo de Android.
 * El usuario elige una fecha y se la devuelve a la pantalla anterior.
 */
public class ElegirFechaDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String ARG_DAY = "day";
    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";

    /**
     * Crea una nueva instancia del diálogo con una fecha inicial.
     */
    public static ElegirFechaDialog newInstance(int day, int month, int year) {
        ElegirFechaDialog frag = new ElegirFechaDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY, day);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Crea el DatePickerDialog con la fecha que se le haya pasado
     * o si no, usa la fecha de hoy.
     */
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

    /**
     * Se llama cuando el usuario pulsa "Aceptar".
     * Envía la fecha elegida al fragmento padre.
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Bundle result = new Bundle();
        result.putInt("year", year);
        result.putInt("month", month);
        result.putInt("day", dayOfMonth);
        getParentFragmentManager().setFragmentResult("fechaSeleccionada", result);
    }
}
