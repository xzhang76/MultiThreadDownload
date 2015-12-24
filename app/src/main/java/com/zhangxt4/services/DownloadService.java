package com.zhangxt4.services;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.zhangxt4.entities.FileInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by zhangxt4 on 2015/12/14.
 */
public class DownloadService extends Service {
    //activity传过来的intent有两个action
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final String ACTION_FINISH = "ACTION_FINISH";

    public static final String DOWN_PATH = Environment.getExternalStorageDirectory()+"/downloads/";
    public static final String LOG_TAG = "MultiThreadDownload";
    public static final int MSG_INIT = 0;
    /* 为了方便对所有的文件对应的DownloadTask进行管理，创建一个map集合
     * key对应文件的id，value对应DownloadTask
     */
    private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<Integer, DownloadTask>(); //下载任务的集合

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_INIT:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    Log.i(LOG_TAG, "Init: "+fileInfo.toString());//应该已经获得文件长度
                    //启动下载任务
                    DownloadTask task = new DownloadTask(DownloadService.this, fileInfo, 3);
                    task.downloadFile();
                    mTasks.put(fileInfo.getId(), task);//添加到集合中
                    break;
            }
        }
    };

    //在这里接收Activity传过来的fileInfo
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent == null))return 0;
        if (ACTION_START == intent.getAction()){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i(LOG_TAG, "Start: "+fileInfo.toString());
            //启动初始化线程来获得文件的长度
//            new InitThread(fileInfo).start();
            DownloadTask.sExecutorService.execute(new InitThread(fileInfo));
        } else if (ACTION_STOP == intent.getAction()){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i(LOG_TAG, "Stop: "+fileInfo.toString());
            //从集合中取出点击文件对应的下载，然后让其停止
            DownloadTask task = mTasks.get(fileInfo.getId());
            if (task != null){
                task.isPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 这个Thread类只是用来获取网络文件的长度
     */
    private class InitThread extends Thread{
        private FileInfo mFileInfo;
        public InitThread(FileInfo fileInfo){
            this.mFileInfo = fileInfo;
        }
        @Override
        public void run() {
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            try{
                //连接网络文件
                conn = (HttpURLConnection) new URL(mFileInfo.getUrl()).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                int length = -1;
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    //获取网络文件长度
                    length = conn.getContentLength();
                }
                if (length <= 0){
                    return;
                }
                File dir = new File(DOWN_PATH);
                if(!dir.exists()){
                    dir.mkdir();
                }
                //本地创建文件
                File file = new File(dir, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                //设置文件长度
                raf.setLength(length);
                mFileInfo.setLength(length);
                //回传给主线程Handler
                mHandler.obtainMessage(MSG_INIT, mFileInfo).sendToTarget();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (raf!=null)raf.close();
                    if (conn!=null)conn.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
