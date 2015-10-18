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

import java.lang.System;
import java.lang.Comparable;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Alarm implements Comparable<Alarm>
{
  private Context mContext;
  private long mId;
  private String mTitle;
  private long mDate;
  private boolean mEnabled;
  private int mOccurence;
  private int mDays;
  private long mNextOccurence;

  public static final int ONCE = 0;
  public static final int WEEKLY = 1;

  public static final int NEVER = 0;
  public static final int EVERY_DAY = 0x7f;

  public Alarm(Context context)
  {
    mContext = context;
    mId = 0;
    mTitle = "";
    mDate = System.currentTimeMillis();
    mEnabled = true;
    mOccurence = ONCE;
    mDays = EVERY_DAY;
    update();
  }

  public long getId()
  {
    return mId;
  }

  public void setId(long id)
  {
    mId = id;
  }

  public String getTitle()
  {
    return mTitle;
  }

  public void setTitle(String title)
  {
    mTitle = title;
  }

  public int getOccurence()
  {
    return mOccurence;
  }

  public void setOccurence(int occurence)
  {
    mOccurence = occurence;
    update();
  }

  public long getDate()
  {
    return mDate;
  }

  public void setDate(long date)
  {
    mDate = date;
    update();
  }

  public boolean getEnabled()
  {
    return mEnabled;
  }

  public void setEnabled(boolean enabled)
  {
    mEnabled = enabled;
  }

  public int getDays()
  {
    return mDays;
  }

  public void setDays(int days)
  {
    mDays = days;
    update();
  }

  public long getNextOccurence()
  {
    return mNextOccurence;
  }

  public boolean getOutdated()
  {
    return mNextOccurence < System.currentTimeMillis();
  }

  public int compareTo(Alarm aThat)
  {
    final long thisNext = getNextOccurence();
    final long thatNext = aThat.getNextOccurence();
    final int BEFORE = -1;
    final int EQUAL = 0;
    final int AFTER = 1;

    if (this == aThat)
      return EQUAL;

    if (thisNext > thatNext)
      return AFTER;
    else if (thisNext < thatNext)
      return BEFORE;
    else
      return EQUAL;
  }

  public void update()
  {
    Calendar now = Calendar.getInstance();

    if (mOccurence == WEEKLY)
    {
      Calendar alarm = Calendar.getInstance();

      alarm.setTimeInMillis(mDate);
      alarm.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

      if (mDays != NEVER)
      {
        while (true)
        {
          int day = (alarm.get(Calendar.DAY_OF_WEEK) + 5) % 7;  

          if (alarm.getTimeInMillis() > now.getTimeInMillis() && (mDays & (1 << day)) > 0)
            break;

          alarm.add(Calendar.DAY_OF_MONTH, 1);
        }
      }
      else
      {
        alarm.add(Calendar.YEAR, 10);
      }

      mNextOccurence = alarm.getTimeInMillis();
    }
    else
    {
      mNextOccurence = mDate;
    }

    mDate = mNextOccurence;
  }

  public void toIntent(Intent intent)
  {
    intent.putExtra("com.taradov.alarmme.id", mId);
    intent.putExtra("com.taradov.alarmme.title", mTitle);
    intent.putExtra("com.taradov.alarmme.date", mDate);
    intent.putExtra("com.taradov.alarmme.alarm", mEnabled);
    intent.putExtra("com.taradov.alarmme.occurence", mOccurence);
    intent.putExtra("com.taradov.alarmme.days", mDays);
  }

  public void fromIntent(Intent intent)
  {
    mId = intent.getLongExtra("com.taradov.alarmme.id", 0);
    mTitle = intent.getStringExtra("com.taradov.alarmme.title");
    mDate = intent.getLongExtra("com.taradov.alarmme.date", 0);
    mEnabled = intent.getBooleanExtra("com.taradov.alarmme.alarm", true);
    mOccurence = intent.getIntExtra("com.taradov.alarmme.occurence", 0);
    mDays = intent.getIntExtra("com.taradov.alarmme.days", 0);
    update();
  }

  public void serialize(DataOutputStream dos) throws IOException
  {
    dos.writeLong(mId);
    dos.writeUTF(mTitle);
    dos.writeLong(mDate);
    dos.writeBoolean(mEnabled);
    dos.writeInt(mOccurence);
    dos.writeInt(mDays);
  }
 
  public void deserialize(DataInputStream dis) throws IOException
  {
    mId = dis.readLong();
    mTitle = dis.readUTF();
    mDate = dis.readLong();
    mEnabled = dis.readBoolean();
    mOccurence = dis.readInt();
    mDays = dis.readInt();
    update();
  }
}

