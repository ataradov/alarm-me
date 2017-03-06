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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.content.Intent;
import android.content.Context;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.widget.Toast;

class AlarmListAdapter extends BaseAdapter
{
  private final String TAG = "AlarmMe";

  private Context mContext;
  private DataSource mDataSource;
  private LayoutInflater mInflater;
  private DateTime mDateTime;
  private int mColorOutdated;
  private int mColorActive;
  private AlarmManager mAlarmManager; 

  public AlarmListAdapter(Context context)
  {
    mContext = context;
    mDataSource = DataSource.getInstance(context);

    Log.i(TAG, "AlarmListAdapter.create()");

    mInflater = LayoutInflater.from(context);
    mDateTime = new DateTime(context);

    mColorOutdated = mContext.getResources().getColor(R.color.alarm_title_outdated);
    mColorActive = mContext.getResources().getColor(R.color.alarm_title_active);

    mAlarmManager = (AlarmManager)context.getSystemService(mContext.ALARM_SERVICE);

    dataSetChanged();
  }

  public void save()
  {
    mDataSource.save();
  }

  public void update(Alarm alarm)
  {
    mDataSource.update(alarm);
    dataSetChanged();
  }

  public void updateAlarms()
  {
    Log.i(TAG, "AlarmListAdapter.updateAlarms()");
    for (int i = 0; i < mDataSource.size(); i++)
      mDataSource.update(mDataSource.get(i));
    dataSetChanged();
  }

  public void add(Alarm alarm)
  {
    mDataSource.add(alarm);
    dataSetChanged();
  }

  public void delete(int index)
  {
    cancelAlarm(mDataSource.get(index));
    mDataSource.remove(index);
    dataSetChanged();
  }

  public void onSettingsUpdated()
  {
    mDateTime.update();
    dataSetChanged();
  }

  public int getCount()
  {
    return mDataSource.size();
  }

  public Alarm getItem(int position)
  {
    return mDataSource.get(position);
  }

  public long getItemId(int position)
  {
    return position;
  }

  public View getView(int position, View convertView, ViewGroup parent)
  {
    ViewHolder holder;
    Alarm alarm = mDataSource.get(position);

    if (convertView == null)
    {
      convertView = mInflater.inflate(R.layout.list_item, null);

      holder = new ViewHolder();
      holder.title = (TextView)convertView.findViewById(R.id.item_title);
      holder.details = (TextView)convertView.findViewById(R.id.item_details);

      convertView.setTag(holder);
    }
    else
    {
      holder = (ViewHolder)convertView.getTag();
    }
  
    holder.title.setText(alarm.getTitle());
    holder.details.setText(mDateTime.formatDetails(alarm) + (alarm.getEnabled() ? "" : " [disabled]"));

    if (alarm.getOutdated())
      holder.title.setTextColor(mColorOutdated);
    else
      holder.title.setTextColor(mColorActive);

    return convertView;
  }

  private void dataSetChanged()
  {
    for (int i = 0; i < mDataSource.size(); i++)
      setAlarm(mDataSource.get(i));

    notifyDataSetChanged();
  }

  private void setAlarm(Alarm alarm)
  {
    PendingIntent sender;
    Intent intent;

    if (alarm.getEnabled() && !alarm.getOutdated())
    {
      intent = new Intent(mContext, AlarmReceiver.class);
      alarm.toIntent(intent);
      sender = PendingIntent.getBroadcast(mContext, (int)alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
      mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm.getDate(), sender);
      Log.i(TAG, "AlarmListAdapter.setAlarm(" + alarm.getId() + ", '" + alarm.getTitle() + "', " + alarm.getDate()+")");
    }
  }

  private void cancelAlarm(Alarm alarm)
  {
    PendingIntent sender;
    Intent intent;

    intent = new Intent(mContext, AlarmReceiver.class);
    sender = PendingIntent.getBroadcast(mContext, (int)alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mAlarmManager.cancel(sender);
  }

  static class ViewHolder
  {
    TextView title;
    TextView details;
  }
}

