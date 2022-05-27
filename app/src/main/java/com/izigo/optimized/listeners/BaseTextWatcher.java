package com.izigo.optimized.listeners;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class BaseTextWatcher implements TextWatcher {

    public BaseTextWatcher() { }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

    @Override
    public void afterTextChanged(Editable editable) { }
}
