package com.example.android.demo;


import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import com.tencent.cls.producer.common.LogContent;
import com.tencent.cls.producer.common.LogItem;
import com.tencent.cls.producer.common.Logs;
import com.tencent.cls.producer.request.PutLogsRequest;
import com.tencent.cls.producer.response.PutLogsResponse;

import java.util.Random;
import java.util.concurrent.Future;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent intent = getIntent();
        String message = intent.getStringExtra("EXTRA_MESSAGE");

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.textView);
        textView.setText(message);
    }

    public void sendMessage(View view) {


        Intent intent = new Intent(this, this.getClass());
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        String msg = sendEvent(message);
        intent.putExtra("EXTRA_MESSAGE", msg);
        startActivity(intent);
    }

    private String sendEvent(String message) {
        // 日志主题ID，必填
        String topicId = "*";
        // 日志源机器IP，可选
        String source = "test_source";
        // 日志源文件名，可选
        String filename = "test_filename";

        int ts = (int) (System.currentTimeMillis() / 1000);
        LogItem logItem = new LogItem(ts);
        logItem.PushBack(new LogContent("__CONTENT__", message));
        logItem.PushBack(new LogContent("city", "guangzhou"));
        logItem.PushBack(new LogContent("logNo",
                String.valueOf(System.currentTimeMillis() + new Random(1000).nextInt())));
        logItem.PushBack(new LogContent("__PKG_LOGID__", (String.valueOf(System.currentTimeMillis()))));
        Logs.LogGroup.Builder logGroup = Logs.LogGroup.newBuilder();
        logGroup.addLogs(logItem.mContents);
        MainApplication app = (MainApplication)getApplicationContext();
        final PutLogsRequest req = new PutLogsRequest(topicId, source, filename, logGroup);
        try {
            Future<PutLogsResponse> resq = app.getAsyncClientInstance().PutLogs(req);
            // resq.get() 是阻塞的
            if (null != resq.get()) {
                return resq.get().GetAllHeaders().toString();
            }
            return "sent message error";
        }catch (Exception e) {
            return e.getMessage();
        }
    }

}