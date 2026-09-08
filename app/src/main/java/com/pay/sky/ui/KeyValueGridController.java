package com.pay.sky.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.pay.sky.R;
import com.pay.sky.data.KeyValuePair;
import com.pay.sky.util.HapticUtil;
import java.util.ArrayList;
import java.util.List;

public class KeyValueGridController {

    private final Context context;
    private final LinearLayout container;
    private final List<KeyValuePair> items = new ArrayList<>();
    private final List<View> rowViews = new ArrayList<>();
    private final List<View> dividerViews = new ArrayList<>();

    public KeyValueGridController(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setItems(List<KeyValuePair> initialItems) {
        items.clear();
        container.removeAllViews();
        rowViews.clear();
        dividerViews.clear();

        if (initialItems != null) {
            for (KeyValuePair pair : initialItems) {
                if (pair != null && (!pair.getKey().trim().isEmpty() || !pair.getValue().trim().isEmpty())) {
                    items.add(new KeyValuePair(pair.getKey(), pair.getValue()));
                }
            }
        }

        // Always ensure exactly one trailing empty row
        items.add(new KeyValuePair("", ""));

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                addDividerView();
            }
            addRowView(i);
        }
    }

    public List<KeyValuePair> getValidItems() {
        List<KeyValuePair> result = new ArrayList<>();
        for (KeyValuePair pair : items) {
            if (pair != null && (!pair.getKey().trim().isEmpty() || !pair.getValue().trim().isEmpty())) {
                result.add(new KeyValuePair(pair.getKey().trim(), pair.getValue().trim()));
            }
        }
        return result;
    }

    private void addDividerView() {
        View divider = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.colorCardBorder));
        container.addView(divider);
        dividerViews.add(divider);
    }

    private void addRowView(final int index) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rowView = inflater.inflate(R.layout.item_key_value_row, container, false);
        container.addView(rowView);
        rowViews.add(rowView);

        EditText etKey = rowView.findViewById(R.id.etRowKey);
        EditText etValue = rowView.findViewById(R.id.etRowValue);
        ImageView ivDelete = rowView.findViewById(R.id.ivDeleteRow);

        KeyValuePair pair = items.get(index);
        etKey.setText(pair.getKey());
        etValue.setText(pair.getValue());

        etKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int currentIndex = rowViews.indexOf(rowView);
                if (currentIndex >= 0 && currentIndex < items.size()) {
                    items.get(currentIndex).setKey(s != null ? s.toString() : "");
                    checkTrailingRowAddition(currentIndex);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int currentIndex = rowViews.indexOf(rowView);
                if (currentIndex >= 0 && currentIndex < items.size()) {
                    items.get(currentIndex).setValue(s != null ? s.toString() : "");
                    checkTrailingRowAddition(currentIndex);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (!hasFocus) {
                cleanUpEmptyMiddleRows();
            }
        };
        etKey.setOnFocusChangeListener(focusListener);
        etValue.setOnFocusChangeListener(focusListener);

        ivDelete.setOnClickListener(v -> {
            HapticUtil.vibrateClick(context);
            deleteRow(rowView);
        });
    }

    private void checkTrailingRowAddition(int changedIndex) {
        int lastIndex = items.size() - 1;
        if (changedIndex == lastIndex) {
            KeyValuePair lastPair = items.get(lastIndex);
            if (!lastPair.getKey().trim().isEmpty() && !lastPair.getValue().trim().isEmpty()) {
                // User filled both key and value of the trailing empty row: automatically append new empty row
                items.add(new KeyValuePair("", ""));
                addDividerView();
                addRowView(items.size() - 1);
            }
        }
    }

    private void cleanUpEmptyMiddleRows() {
        if (items.size() <= 1) {
            return;
        }

        boolean removed = false;
        // Check rows except the very last trailing one
        for (int i = items.size() - 2; i >= 0; i--) {
            KeyValuePair pair = items.get(i);
            if (pair.isEmpty()) {
                removeRowAtIndex(i);
                removed = true;
            }
        }
    }

    private void deleteRow(View targetRowView) {
        int index = rowViews.indexOf(targetRowView);
        if (index < 0) return;

        int lastIndex = items.size() - 1;
        if (index == lastIndex) {
            // Trailing empty row - just clear fields
            EditText etKey = targetRowView.findViewById(R.id.etRowKey);
            EditText etValue = targetRowView.findViewById(R.id.etRowValue);
            etKey.setText("");
            etValue.setText("");
            items.get(lastIndex).setKey("");
            items.get(lastIndex).setValue("");
        } else {
            removeRowAtIndex(index);
        }
    }

    private void removeRowAtIndex(int index) {
        if (index < 0 || index >= items.size()) return;

        items.remove(index);
        View rowView = rowViews.remove(index);
        container.removeView(rowView);

        if (!dividerViews.isEmpty()) {
            int dividerIndex = (index > 0) ? (index - 1) : 0;
            if (dividerIndex < dividerViews.size()) {
                View divider = dividerViews.remove(dividerIndex);
                container.removeView(divider);
            }
        }

        // Ensure there is always at least one trailing empty row
        if (items.isEmpty() || !items.get(items.size() - 1).isEmpty()) {
            items.add(new KeyValuePair("", ""));
            if (rowViews.size() > 0) {
                addDividerView();
            }
            addRowView(items.size() - 1);
        }
    }
}
