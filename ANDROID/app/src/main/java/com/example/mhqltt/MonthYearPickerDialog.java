package com.example.mhqltt;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.view.ViewGroup.LayoutParams;
import java.util.Calendar;

public class MonthYearPickerDialog {

    public interface OnDateSetListener {
        void onDateSet(int year, int month);
    }

    private final OnDateSetListener listener;
    private final AlertDialog dialog;
    private final NumberPicker monthPicker;
    private final NumberPicker yearPicker;

    public MonthYearPickerDialog(Context context, final OnDateSetListener listener) {
        this.listener = listener;

        // Set up the month and year pickers
        monthPicker = new NumberPicker(context);
        yearPicker = new NumberPicker(context);

        // Set up the month picker
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"});

        // Set up the year picker
        int year = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(year - 100);
        yearPicker.setMaxValue(year + 100);
        yearPicker.setValue(year);

        // Set up the layout for the dialog
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        params.weight = 1;

        ll.addView(monthPicker, params);
        ll.addView(yearPicker, params);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Chọn tháng và năm");
        builder.setView(ll);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onDateSet(yearPicker.getValue(), monthPicker.getValue());
            }
        });
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }
}
