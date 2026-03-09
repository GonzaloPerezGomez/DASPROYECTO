package com.example.dasproyecto;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Preferencia personalizada que muestra círculos de colores
 * para que el usuario elija el color secundario de la app.
 */
public class ColorSelectorPreference extends Preference {

    // Colores disponibles: azul, rojo, verde, naranja, morado
    private static final String[] COLOR_KEYS = {
            "azul", "rojo", "verde", "naranja", "morado"
    };

    private static final int[] COLOR_VALUES = {
            Color.parseColor("#1976D2"), // azul
            Color.parseColor("#D32F2F"), // rojo
            Color.parseColor("#388E3C"), // verde
            Color.parseColor("#F57C00"), // naranja
            Color.parseColor("#7B1FA2"), // morado
    };

    private static final int CIRCLE_SIZE_DP = 40;
    private static final int CIRCLE_MARGIN_DP = 12;
    private static final int STROKE_WIDTH_DP = 3;

    // Un ID único para nuestro contenedor creado programáticamente
    private static final int CONTAINER_ID = 1000101;

    private String selectedColor;

    public ColorSelectorPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_color_selector);
    }

    public ColorSelectorPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorSelectorPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorSelectorPreference(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Saca el valor por defecto del XML de preferencias.
     *
     * @param a     Array de atributos tipados.
     * @param index Posición del valor.
     * @return El valor por defecto como String.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /**
     * Pone el valor inicial de la preferencia.
     * Si ya hay un color guardado lo usa, si no, usa el valor por defecto.
     *
     * @param defaultValue Valor por defecto.
     */
    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        selectedColor = getPersistedString(
                defaultValue != null ? defaultValue.toString() : "azul");
    }

    /**
     * Dibuja los círculos de colores en pantalla.
     * Marca con un check el color que esté seleccionado.
     *
     * @param holder ViewHolder con las vistas de la preferencia.
     */
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setClickable(false);

        LinearLayout container = (LinearLayout) holder.findViewById(R.id.color_buttons_container);
        if (container == null)
            return;

        container.removeAllViews();

        int circleSizePx = dpToPx(CIRCLE_SIZE_DP);
        int marginPx = dpToPx(CIRCLE_MARGIN_DP);
        int strokePx = dpToPx(STROKE_WIDTH_DP);

        for (int i = 0; i < COLOR_KEYS.length; i++) {
            final String colorKey = COLOR_KEYS[i];
            final int colorValue = COLOR_VALUES[i];

            ImageView circle = new ImageView(getContext());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    circleSizePx, circleSizePx);
            if (i > 0) {
                params.setMarginStart(marginPx);
            }
            circle.setLayoutParams(params);

            // Drawable circular
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(colorValue);

            // Si está seleccionado, añadir borde
            if (colorKey.equals(selectedColor)) {
                drawable.setStroke(strokePx, Color.WHITE);
                circle.setElevation(dpToPx(4));
            }

            circle.setBackground(drawable);

            // Icono de check para el seleccionado
            if (colorKey.equals(selectedColor)) {
                circle.setImageResource(R.drawable.ic_check);
                circle.setColorFilter(Color.WHITE);
                circle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                circle.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            }

            circle.setContentDescription(colorKey);
            circle.setClickable(true);
            circle.setFocusable(true);

            circle.setOnClickListener(v -> {
                selectedColor = colorKey;
                persistString(colorKey);
                notifyChanged();
            });

            container.addView(circle);
        }
    }

    /**
     * Convierte dp a píxeles según la densidad de la pantalla.
     *
     * @param dp Valor en dp.
     * @return Valor en píxeles.
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
