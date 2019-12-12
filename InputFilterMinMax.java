package com.exercise.currencyconverter;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by billy on 13/12/2017.
 */

public class InputFilterMinMax implements InputFilter {
    private double min, max;

    public InputFilterMinMax(double min, double max){
        this.min = min;
        this.max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            double input = Double.parseDouble(dest.toString() + source.toString());
            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException nfe) { }
        return "";
    }

    private boolean isInRange(double a, double b, double c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}
