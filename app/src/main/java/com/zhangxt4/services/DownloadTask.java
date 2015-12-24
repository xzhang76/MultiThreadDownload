package com.zhangxt4.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.zhangxt4.db.ThreadDAO;
import com.zhangxt4.db.ThreadDAOImpl;
import com.zhangxt4.entities.FileInfo;
import com.zhangxt4.entities.ThreadInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载文件任务类
 * 一个文件对应一个DownloadTask，而一个DownloadTask又由多个线程来共同下载一个文件
 * 注意：DownloadService类中线程类只是获取网络文件长度的作用
 * Created by zhangxt4 on 2015/12/15.
 */
public class DownloadTask {
    private ThreadDAO mDao;
    private Context mContext;
    private FileInfo mFileInfo;
    private int mFinished = 0; //保存整个文件的下载进度
    public boolean isPause;
    private int mThreadCount = 1; //线程数
    //每个DownloadTask都有多个线程，为了使暂停时能使其所有线程都停止，这里创建一个线程的集合便于统一管理
    private List<DownloadThread> mThreads;
    public static ExecutorService sExecutorService = Executors.newCachedThreadPool(); //创建一个线程池

    public DownloadTask(Context context, FileInfo fileInfo, int threadCount){
        this.mContext = context;
        this.mFileInfo = fileInfo;
        this.mThreadCount = threadCount;
        mDao = new ThreadDAOImpl(context);
    }
    public void downloadFile(){
        //读取上次下载的信息
        List<ThreadInfo> threadInfos = mDao.getThread(mFileInfo.getUrl());
        if(threadInfos.size() == 0){
            //还没开始下载，新创建多个线程信息，分别完成各自的下载长度
            int length = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i++){
                ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), i*length,(i+1)*length - 1, 0);
                if (i == mThreadCount - 1){
                    //最后一个线程
                    threadInfo.setEnd(mFileInfo.getLength());
                }
                //添加到线程信息集合中
                threadInfos.add(threadInfo);
                //向数据库中插入线程信息
                mDao.insertThread(threadInfo);
            }
        }
        /* 创建多个子线程并开始下载(线程数由DownloadService调用构造函数指定)
         * 注意：这里的threadInfos线程信息集合可能是数据库中读出来的（已经下载一部分了），也可能是新创建的
         */
        mThreads = new ArrayList<DownloadThread>();
        for (ThreadInfo info: threadInfos){
            DownloadThread downloadThread = new DownloadThread(info);
//            downloadThread.start();
            DownloadTask.sExecutorService.execute(downloadThread); //取出线程放入线程池中执行
            mThreads.add(downloadThread);
        }
    }

    /**
     * 判断所有线程是否都已执行完毕，thread.isFinished初始值是false，只有执行完才会置为true
     * 若都已经执行完表示文件已经下载完毕，发送一个广播给主线程，把已经下载完的文件回传
     */
    private synchronized void checkAllThreadsFinished(){
        boolean allFinished = true;
        //遍历线程集合，判断是否都已执行完毕
        for (DownloadThread thread: mThreads){
            if (!thread.isFinished){
                allFinished = false;
                break;
            }
        }
        if (allFinished){
            //删除线程信息
            mDao.deleteThread(mFileInfo.getUrl());
            //所有都已经执行完毕，发送广播给Service告诉其当前文件已经下载完毕
            Intent intent = new Intent(DownloadService.ACTION_FINISH);
            intent.putExtra("fileInfo", mFileInfo); //将当前下载完的文件回传给主线程
            mContext.sendBroadcast(intent);
        }
    }
    /**
     * 真正的下载线程
     */
    public class DownloadThread extends Thread{
        private ThreadInfo mThreadInfo;
        private boolean isFinished = false; //标识当前下载线程已经执行完毕

        public DownloadThread(ThreadInfo threadInfo) {
            this.mThreadInfo = threadInfo;
        }

        @Override
        public void run() {
            Log.i(DownloadService.LOG_TAG, "DownloadThread start:run(): "+mThreadInfo.toString());
            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            InputStream is = null;
            try{
                conn = (HttpURLConnection) new URL(mThreadInfo.getUrl()).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                //2.设置下载位置
                int start = mThreadInfo.getStart()+mThreadInfo.getFinished();
                conn.setRequestProperty("Range", "bytes=" + start + "-" + mThreadInfo.getEnd());
                //3.设置文件写入位置
                File file = new File(DownloadService.DOWN_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start); //从start位置开始写
                //4.开始下载
                Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                mFinished += mThreadInfo.getFinished();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL){
                    //读取数据
                    byte[] buffer = new byte[1024*4];
                    is = conn.getInputStream();
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while((len = is.read(buffer)) != -1){
                        raf.write(buffer, 0, len);
                        //5.把下载进度通过发送广播来更新给主线程
                        mFinished += len; //累加整个文件的下载进度
                        mThreadInfo.setFinished(mThreadInfo.getFinished() + len); //累加每个线程的下载进度
                        if ((System.currentTimeMillis() - time) > 1000) {
                            //1s才会发送一个广播
                            time = System.currentTimeMillis();
                            intent.putExtra("id", mFileInfo.getId());
                            intent.putExtra("finished", mFinished * 100 / mFileInfo.getLength());
                            mContext.sendBroadcast(intent);
                        }
                        //在下载暂停时，保存每个线程当前的下载进度
                        if (isPause){
                            mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
                            Log.i(DownloadService.LOG_TAG, "DownloadThread pause: run(): " + mThreadInfo.toString());
                            return;
                        }
                    }
                    //下载完成
                    isFinished = true;
                    checkAllThreadsFinished(); //每执行完一个线程就会判断是否都已执行完
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (raf!=null)raf.close();
                    if (conn!=null)conn.disconnect();
                    if (is!=null)is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
