package com.one.whatsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class AddTask extends AppCompatActivity {
    int hr=100,min=100,sec=100,days=1;
    String time_of_exe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        EditText text=(EditText) findViewById(R.id.getNum);
        EditText text2=(EditText) findViewById(R.id.getText);

        ImageView btn=findViewById(R.id.button);
        ImageView set=findViewById(R.id.set);
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference().child("Users").child("Data").child(getIntent().getSerializableExtra("user").toString());


        set.setOnClickListener(view -> {
            TimePickerDialog picker=TimePickerDialog.newInstance(
                    (view1, hourOfDay, minute, second) -> {
                        hr=hourOfDay;
                        min=minute;
                        sec=second;
                        time_of_exe =hr+":"+min+":"+sec+":";
                    },false
            );
            picker.show(getSupportFragmentManager(),"DatePickerDialog");
        });

        btn.setOnClickListener(view -> {

            Editable msg = text2.getText();
            Editable number = text.getText();
            long flexTime = CalculateFlex(hr, min, sec, days);

            String num = number.toString();
            String text1 = msg.toString() ;

            if((hr==100&&sec==100)&&(!num.isEmpty()&&!text1.isEmpty())){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                try {
                    intent.setData(Uri.parse("http://api.whatsapp.com/send?phone=+91"+num+"&text="+ URLEncoder.encode(text1,"UTF-8")+"   "));
                    intent.setPackage("com.whatsapp");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                    ref.child("Instantly").setValue(new Task(num,text1,"Instantly","Success"));
                    finish();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else if((hr!=100&&sec!=100)&&(!num.isEmpty()&&!text1.isEmpty())){
            boolean installed = isAppInstalled("com.whatsapp");

            if (installed) {



                Data data = new Data.Builder()
                        .putString("number", "+91"+num)
                        .putString("Text", text1 + "   ")
                        .putString("time", time_of_exe)
                        .putString("user",getIntent().getSerializableExtra("user").toString())
                        .build();
                PeriodicWorkRequest sendMessage = new PeriodicWorkRequest.Builder(WhatsAppWorker.class
                        , days, TimeUnit.DAYS, flexTime, TimeUnit.MILLISECONDS).setInputData(data)
                        .addTag("PeriodicWorker")
                        .build();

                WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("PeriodicWorker",
                        ExistingPeriodicWorkPolicy.REPLACE, sendMessage);

                ref.child(time_of_exe).setValue(new Task(num, text1, time_of_exe, "Pending"));
                Toast.makeText(AddTask.this, "Work Request Sent", Toast.LENGTH_LONG).show();
                finish();

            } else {
                Toast.makeText(AddTask.this, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show();
            }
            
        }
            else{
                Toast.makeText(AddTask.this,"Please Give Valid Data",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private long CalculateFlex(int hour, int minute, int second, int periodInDays){
        Calendar call=Calendar.getInstance();
        call.set(Calendar.HOUR_OF_DAY,hour);
        call.set(Calendar.MINUTE,minute);
        call.set(Calendar.SECOND,second);

        Calendar Cal2=Calendar.getInstance();
        if(Cal2.getTimeInMillis()<call.getTimeInMillis()){
            Cal2.setTimeInMillis(Cal2.getTimeInMillis()+ TimeUnit.DAYS.toMillis(periodInDays));

        }
        long delta=(Cal2.getTimeInMillis() - call.getTimeInMillis());
        return ((delta> PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS)?delta:PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS);
    }
    private boolean isAppInstalled(String s) {
        PackageManager packageManager = getPackageManager();
        boolean is_installed;

        try {
            packageManager.getPackageInfo(s, PackageManager.GET_ACTIVITIES);
            is_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            is_installed = false;
            e.printStackTrace();
        }
        return is_installed;
    }
}