package com.zhangxt4.multithreaddownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import com.zhangxt4.entities.FileInfo;
import com.zhangxt4.services.DownloadService;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private ListView mListView;
    private List<FileInfo> mFileList; //数据源
    private FileListAdapter mAdapter; //ListView的适配器
    private static final String DOWNLOAD_URL1 = "http://www.imooc.com/mobile/mukewang.apk";
    private static final String DOWNLOAD_URL2 = "http://static.kunlun.com/bb/coc/Clash_of_Clans-8.67.8-kunlun_landing_page-release.apk";
    private static final String DOWNLOAD_URL3 = "http://csdn-app.csdn.net/csdn.apk";
    private static final String DOWNLOAD_URL4 = "http://app.jikexueyuan.com/GeekAcademy_release_jikexueyuan_aligned.apk";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())){
                int finished = intent.getIntExtra("finished", 0);
                int id = intent.getIntExtra("id", 0);
                /* 因为现在是多个任务，当收到相关广播时，需要指定是哪个文件，然后才能设置其进度条
                 * 需要在DownloadTask中发送广播更新进度条时，不仅要发送已完成进度，还要发送文件的id用以标识
                 */
                mAdapter.updateProgress(id, finished);
            }else if (DownloadService.ACTION_FINISH.equals(intent.getAction())){
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                mAdapter.updateProgress(fileInfo.getId(), 100);
                Toast.makeText(MainActivity.this, fileInfo.getFileName()+"下载完毕", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listView);
        mFileList = new ArrayList<FileInfo>();
        initDatas();
        initEvents();
        //创建适配器并设置给ListView
        mAdapter = new FileListAdapter(MainActivity.this, mFileList);
        mListView.setAdapter(mAdapter);
    }

    private void initDatas() {
        FileInfo fileInfo1 = new FileInfo(0, DOWNLOAD_URL1, DOWNLOAD_URL1.substring(DOWNLOAD_URL1.lastIndexOf("/") + 1), 0, 0);
        FileInfo fileInfo2 = new FileInfo(1, DOWNLOAD_URL2, DOWNLOAD_URL2.substring(DOWNLOAD_URL2.lastIndexOf("/") + 1), 0, 0);
        FileInfo fileInfo3 = new FileInfo(2, DOWNLOAD_URL3, DOWNLOAD_URL3.substring(DOWNLOAD_URL3.lastIndexOf("/") + 1), 0, 0);
        FileInfo fileInfo4 = new FileInfo(3, DOWNLOAD_URL4, DOWNLOAD_URL4.substring(DOWNLOAD_URL4.lastIndexOf("/") + 1), 0, 0);
        mFileList.add(fileInfo1);
        mFileList.add(fileInfo2);
        mFileList.add(fileInfo3);
        mFileList.add(fileInfo4);
    }

    private void initEvents() {
        //注册广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISH);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
