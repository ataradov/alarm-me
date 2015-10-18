/**************************************************************************
 *
 * Copyright (C) 2012-2015 Alex Taradov <alex@taradov.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *************************************************************************/

package com.taradov.alarmme;

import java.util.Date;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import com.taradov.alarmme.Alarm;
import com.taradov.alarmme.DateTime;

public class EditAlarm extends Activity
{
  private EditText mTitle;
  private CheckBox mAlarmEnabled;
  private Spinner mOccurence;
  private Button mDateButton;
  private Button mTimeButton;

  private Alarm mAlarm;
  private DateTime mDateTime;

  private GregorianCalendar mCalendar;
  private int mYear;
  private int mMonth;
  private int mDay;
  private int mHour;
  private int mMinute;

  static final int DATE_DIALOG_ID = 0;
  static final int TIME_DIALOG_ID = 1;
  static final int DAYS_DIALOG_ID = 2;

  @Override
  public void onCreate(Bundle bundle)
  {
    super.onCreate(bundle);
    setContentView(R.layout.edit);

    mTitle = (EditText)findViewById(R.id.title);
    mAlarmEnabled = (CheckBox)findViewById(R.id.alarm_checkbox);
    mOccurence = (Spinner)findViewById(R.id.occurence_spinner);
    mDateButton = (Button)findViewById(R.id.date_button);
    mTimeButton = (Button)findViewById(R.id.time_button);

    mAlarm = new Alarm(this);
    mAlarm.fromIntent(getIntent());

    mDateTime = new DateTime(this);

    mTitle.setText(mAlarm.getTitle());
    mTitle.addTextChangedListener(mTitleChangedListener);

    mOccurence.setSelection(mAlarm.getOccurence());
    mOccurence.setOnItemSelectedListener(mOccurenceSelectedListener); 

    mAlarmEnabled.setChecked(mAlarm.getEnabled());
    mAlarmEnabled.setOnCheckedChangeListener(mAlarmEnabledChangeListener); 

    mCalendar = new GregorianCalendar();
    mCalendar.setTimeInMillis(mAlarm.getDate());
    mYear = mCalendar.get(Calendar.YEAR);
    mMonth = mCalendar.get(Calendar.MONTH);
    mDay = mCalendar.get(Calendar.DAY_OF_MONTH);
    mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
    mMinute = mCalendar.get(Calendar.MINUTE);

    updateButtons();
  }

  @Override
  protected Dialog onCreateDialog(int id)
  {
    if (DATE_DIALOG_ID == id)
      return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
    else if (TIME_DIALOG_ID == id)
      return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, mDateTime.is24hClock());
    else if (DAYS_DIALOG_ID == id)
      return DaysPickerDialog();
    else
      return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog)
  {
    if (DATE_DIALOG_ID == id)
      ((DatePickerDialog)dialog).updateDate(mYear, mMonth, mDay);
    else if (TIME_DIALOG_ID == id)
      ((TimePickerDialog)dialog).updateTime(mHour, mMinute);
  }    

  public void onDateClick(View view)
  {
    if (Alarm.ONCE == mAlarm.getOccurence())
      showDialog(DATE_DIALOG_ID);
    else if (Alarm.WEEKLY == mAlarm.getOccurence())
      showDialog(DAYS_DIALOG_ID);
  }

  public void onTimeClick(View view)
  {
    showDialog(TIME_DIALOG_ID);
  }

  public void onDoneClick(View view)
  {
    Intent intent = new Intent();

    mAlarm.toIntent(intent);
    setResult(RESULT_OK, intent);
    finish();
  }

  public void onCancelClick(View view)
  {
    setResult(RESULT_CANCELED, null);  
    finish();
  }

  private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener()
  {
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
    {
      mYear = year;
      mMonth = monthOfYear;
      mDay = dayOfMonth;

      mCalendar = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute);
      mAlarm.setDate(mCalendar.getTimeInMillis());

      updateButtons();
    }
  };

  private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener()
  {
    public void onTimeSet(TimePicker view, int hourOfDay, int minute)
    {
      mHour = hourOfDay;
      mMinute = minute;

      mCalendar = new GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute);
      mAlarm.setDate(mCalendar.getTimeInMillis());

      updateButtons();
    }
  };

  private TextWatcher mTitleChangedListener = new TextWatcher()
  {
    public void afterTextChanged(Editable s)
    {
      mAlarm.setTitle(mTitle.getText().toString());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
    }
  }; 

  private AdapterView.OnItemSelectedListener mOccurenceSelectedListener = new AdapterView.OnItemSelectedListener()
  {
    @Override
    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) 
    {
      mAlarm.setOccurence(position);
      updateButtons();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
    }
  };

  private CompoundButton.OnCheckedChangeListener mAlarmEnabledChangeListener = new CompoundButton.OnCheckedChangeListener()
  {
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
      mAlarm.setEnabled(isChecked);
    }
  };

  private void updateButtons()
  {
    if (Alarm.ONCE == mAlarm.getOccurence())
      mDateButton.setText(mDateTime.formatDate(mAlarm));
    else if (Alarm.WEEKLY == mAlarm.getOccurence())
      mDateButton.setText(mDateTime.formatDays(mAlarm));
    mTimeButton.setText(mDateTime.formatTime(mAlarm));
  }

  private Dialog DaysPickerDialog()
  {
    AlertDialog.Builder builder;
    final boolean[] days = mDateTime.getDays(mAlarm);
    final String[] names = mDateTime.getFullDayNames();

    builder = new AlertDialog.Builder(this);

    builder.setTitle("Week days");

    builder.setMultiChoiceItems(names, days, new DialogInterface.OnMultiChoiceClickListener()
    {
      public void onClick(DialogInterface dialog, int whichButton, boolean isChecked)
      {
      }
    });

    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
    {
      public void onClick(DialogInterface dialog, int whichButton)
      {
        mDateTime.setDays(mAlarm, days);
        updateButtons();
      }
    });

    builder.setNegativeButton("Cancel", null);

    return builder.create();
  }
}

