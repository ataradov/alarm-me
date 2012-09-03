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

import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.net.Uri;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.view.WindowManager;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.media.Ringtone;
import android.media.RingtoneManager;

import android.widget.TextView;

public class AlarmNotification extends Activity
{
  private Ringtone mRingtone;
  private Vibrator mVibrator;
  private final long[] mVibratePattern = { 0, 500, 500 };
  private boolean mVibrate;
  private Uri mAlarmSound;
  private long mPlayTime;
  private Timer mTimer;
  private Alarm mAlarm;
  private DateTime mDateTime;
  private TextView mTextView;

  @Override
  protected void onCreate(Bundle bundle)
  {
    super.onCreate(bundle);

    getWindow().addFlags(
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

    setContentView(R.layout.notification);

    mDateTime = new DateTime(this);

    mAlarm = new Alarm(this);
    mAlarm.fromIntent(getIntent());

    mTextView = (TextView)findViewById(R.id.alarm_title_text);
    mTextView.setText(mAlarm.getTitle());

    readPreferences();

    mRingtone = RingtoneManager.getRingtone(getApplicationContext(), mAlarmSound);
    mRingtone.play();

    if (mVibrate)
    {
      mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
      mVibrator.vibrate(mVibratePattern, 0);
    }

    mTimer = new Timer();
    mTimer.schedule(mTimerTask, mPlayTime);
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    dismiss();
  }

  public void onDismissClick(View view)
  {
    dismiss();
  }

  private void dismiss()
  {
    mTimer.cancel();
    mRingtone.stop();
    if (mVibrate)
      mVibrator.cancel();
    finish();
  }

  private void readPreferences()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    mAlarmSound = Uri.parse(prefs.getString("alarm_sound_pref", "DEFAULT_RINGTONE_URI"));
    mVibrate = prefs.getBoolean("vibrate_pref", true);
    mPlayTime = (long)Integer.parseInt(prefs.getString("alarm_play_time_pref", "30")) * 1000;
  }

  private TimerTask mTimerTask = new TimerTask()
  {
    public void run()
    {
      addNotification();
      dismiss();
    }
  };

  private void addNotification()
  {
    NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification;
    PendingIntent activity;
    Intent intent;

    notification = new Notification(R.drawable.ic_notification, "Missed alarm", System.currentTimeMillis());
    notification.flags |= Notification.FLAG_AUTO_CANCEL;

    intent = new Intent(this, AlarmMe.class);
    intent.setAction("android.intent.action.MAIN");
    intent.addCategory("android.intent.category.LAUNCHER");     

    activity = PendingIntent.getActivity(this, (int)mAlarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    notification.setLatestEventInfo(this, "Missed alarm: " + mAlarm.getTitle(), mDateTime.formatDetails(mAlarm), activity);

    notificationManager.notify(0, notification);            
  }

  @Override
  public void onBackPressed()
  {
  }
}

