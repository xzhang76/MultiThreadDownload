package com.zhangxt4.multithreaddownload;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.zhangxt4.entities.FileInfo;
import com.zhangxt4.services.DownloadService;

import java.util.List;

/**
 * ListView文件列表的适配器
 * Created by zhangxt4 on 2015/12/21.
 */
public class FileListAdapter extends BaseAdapter {
    private Context mContext;
    private List<FileInfo> mFileList; //ListView的数据源
    private LayoutInflater mInflater;

    public FileListAdapter(Context context, List<FileInfo> fileList) {
        this.mContext = context;
        this.mFileList = fileList;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        final boolean[] isDownloading = {false};
        final FileInfo fileInfo = mFileList.get(position);
        if (convertView == null){
            convertView = mInflater.inflate(R.layout.list_item, null); //将layout布局转换成view视图
            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.textView);
            viewHolder.btToggle = (Button) convertView.findViewById(R.id.btToggle);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            //初始化并设置视图中的控件
            viewHolder.textView.setText(fileInfo.getFileName());
            viewHolder.progressBar.setMax(100);
            //为每个FileInfo对应的视图添加一个点击监听
            final Button mToggleButton = viewHolder.btToggle;
            viewHolder.btToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isDownloading[0]) {
                        Intent intentStop = new Intent(mContext, DownloadService.class);
                        intentStop.setAction(DownloadService.ACTION_STOP);
                        intentStop.putExtra("fileInfo", fileInfo);
                        mContext.startService(intentStop);
                        isDownloading[0] = false;
                        mToggleButton.setBackgroundResource(R.mipmap.play);
                    } else {
                        Intent intentStart = new Intent(mContext, DownloadService.class);
                        intentStart.setAction(DownloadService.ACTION_START);
                        intentStart.putExtra("fileInfo", fileInfo);
                        mContext.startService(intentStart);
                        isDownloading[0] = true;
                        mToggleButton.setBackgroundResource(R.mipmap.pause);
                    }
                }
            });
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.progressBar.setProgress(fileInfo.getFinished());
        if (fileInfo.getFinished() == 100){
            viewHolder.textView.setText("下载完毕！");
            viewHolder.btToggle.setBackgroundResource(R.mipmap.done);
            viewHolder.btToggle.setClickable(false);
        }
        return convertView;
    }

    /**
     * 更新列表项中的某个文件的下载进度
     * 调用notifyDataSetChanged()会使Adapter重新加载，getView()会重新执行
     * 然后对应View的progressBar.setProgress(fileInfo.getFinished())就会设置其完成进度
     * @param position DownloadTask回传过来的文件的id
     * @param progress DownloadTask回传过来的已完成进度
     */
    public void updateProgress(int position, int progress){
        FileInfo fileInfo = mFileList.get(position);
        fileInfo.setFinished(progress);
        notifyDataSetChanged(); //整个adapter会被重新加载
    }

    /*
     * 定义为静态类，在程序运行期间只会加载一次
     */
    static class ViewHolder{
        TextView textView;
        Button btToggle;
        ProgressBar progressBar;
    }
}
