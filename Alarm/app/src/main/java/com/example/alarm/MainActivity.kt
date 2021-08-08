package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initOnOffButton()
        initChangeAlarmTimeButton()

        val model = fetchDataFromSharedPreferences()
        renderView(model)


    }

    private fun initOnOffButton(){
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {

            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            if (newModel.onOff){
                // 켜진 경우 -> 알람 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    // 현재 시간 보다 이전이면 DATE 1 증가
                    if (before(Calendar.getInstance())){
                        add(Calendar.DATE, 1)
                    }
                }
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                val intent = Intent(this,AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                // 알람 매니저 사용
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )


            }else{
                // 꺼진 경우 -> 알람 제거
                cancelAlarm()
            }

        }
    }

    private fun initChangeAlarmTimeButton(){
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmTimeButton)
        changeAlarmButton.setOnClickListener {

            // 현재 시간을 가져옴
            val calender = Calendar.getInstance()

            // 데이터 저장
            TimePickerDialog(this,{ picker, hour, minute ->
                val model = saveAlarmModel(hour, minute, false)

                // 뷰 업데이트
                renderView(model)

                // 기존 알람 삭제
                cancelAlarm()

            },calender.get(Calendar.HOUR_OF_DAY), calender.get(Calendar.MINUTE), false)
                .show()
        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel{
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )
        // sharedPreferences 이용해 데이터 저장
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()){
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ON_OFF_KEY, model.onOff)
            commit()
        }
        return model
    }

    // SharedPreferences 에서 데이터 가져오기
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel{
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ON_OFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        // 예외처리
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        // 알람은 꺼져있는데, 데이터는 켜져있는 경우
        if ((pendingIntent == null) and alarmModel.onOff){
            alarmModel.onOff = false
        }
        // 알람은 켜져있는데, 데이터가 꺼져있는 경우 -> 알람 취소
        else if ((pendingIntent != null) and alarmModel.onOff.not()){
            pendingIntent.cancel()
        }
        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel){
        findViewById<TextView>(R.id.amPmTextView).apply {
            text = model.amPmText
        }

        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }

        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm(){
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    companion object{
        private const val ALARM_KEY = "alarm"
        private const val ON_OFF_KEY = "onOFF"
        private const val SHARED_PREFERENCES_NAME = "tiem"
        private const val ALARM_REQUEST_CODE = 1000
    }
}