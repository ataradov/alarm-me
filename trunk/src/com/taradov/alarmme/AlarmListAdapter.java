/**************************************************************************
 *
 * Copyright (C) 2012 Alex Taradov <taradov@gmail.com>
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

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
  private Context mContext;
  private static ArrayList<Alarm> mList;
  private LayoutInflater mInflater;
  private DateTime mDateTime;
  private long mNextId;
  private int mColorOutdated;
  private int mColorActive;
  private AlarmManager mAlarmManager; 

  private final String DATA_FILE_NAME = "alarmme.txt";
  private final long MAGIC_NUMBER = 0x54617261646f7641L;

  public AlarmListAdapter(Context context)
  {
    mContext = context;
    mList = new ArrayList<Alarm>();
    mInflater = LayoutInflater.from(context);
    mDateTime = new DateTime(context);
    mNextId = 1;

    mColorOutdated = mContext.getResources().getColor(R.color.alarm_title_outdated);
    mColorActive = mContext.getResources().getColor(R.color.alarm_title_active);

    mAlarmManager = (AlarmManager)context.getSystemService(mContext.ALARM_SERVICE);

    load();
  }

  public void save()
  {
    try
    {
      DataOutputStream dos = new DataOutputStream(mContext.openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE));

      dos.writeLong(MAGIC_NUMBER);
      dos.writeLong(mNextId);
      dos.writeInt(mList.size());

      for (int i = 0; i < mList.size(); i++)
        mList.get(i).serialize(dos);

      dos.close();
    } catch (IOException e)
    {
    }
  }

  public void load()
  {
    mList.clear();

    try
    {
      DataInputStream dis = new DataInputStream(mContext.openFileInput(DATA_FILE_NAME));
      long magic = dis.readLong();
      int size;

      if (MAGIC_NUMBER == magic)
      {
        mNextId = dis.readLong();
        size = dis.readInt();

        for (int i = 0; i < size; i++)
        {
          Alarm alarm = new Alarm(mContext);
          alarm.deserialize(dis);
          mList.add(alarm);
        }
      }

      dis.close();
    } catch (IOException e)
    {
    }

    dataSetChanged();
  }

  public void update(Alarm alarm)
  {
    dataSetChanged();
  }

  public void updateAlarms()
  {
    for (int i = 0; i < mList.size(); i++)
      mList.get(i).update();
    dataSetChanged();
  }

  public void add(Alarm alarm)
  {
    alarm.setId(mNextId++);
    mList.add(alarm);
    dataSetChanged();
  }

  public void delete(int index)
  {
    cancelAlarm(mList.get(index));
    mList.remove(index);
    dataSetChanged();
  }

  public void onSettingsUpdated()
  {
    mDateTime.update();
    dataSetChanged();
  }

  public int getCount()
  {
    return mList.size();
  }

  public Alarm getItem(int position)
  {
    return mList.get(position);
  }

  public long getItemId(int position)
  {
    return position;
  }

  public View getView(int position, View convertView, ViewGroup parent)
  {
    ViewHolder holder;
    Alarm alarm = mList.get(position);

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
    Collections.sort(mList);
    startSystemAlarms();
    notifyDataSetChanged();
  }

  private void startSystemAlarms()
  {
    for (int i = 0; i < mList.size(); i++)
    {
      Alarm alarm = mList.get(i);

      if (alarm.getEnabled() && !alarm.getOutdated())
        setAlarm(alarm);
    }
  }

  private void setAlarm(Alarm alarm)
  {
    PendingIntent sender;
    Intent intent;

    intent = new Intent(mContext, AlarmReceiver.class);
    alarm.toIntent(intent);
    sender = PendingIntent.getBroadcast(mContext, (int)alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    mAlarmManager.set(AlarmManager.RTC_WAKEUP, alarm.getDate(), sender);
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

