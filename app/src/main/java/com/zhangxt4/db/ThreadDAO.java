package com.zhangxt4.db;

import com.zhangxt4.entities.ThreadInfo;

import java.util.List;

/**
 * 数据库访问和操作接口
 * 用来对表中所包含的的进程信息进行操作(增删查改)
 * Created by zhangxt4 on 2015/12/15.
 */
public interface ThreadDAO {

    /**
     * 添加线程信息
     */
    public void insertThread(ThreadInfo threadInfo);

    /**
     * 删除线程信息
     */
    public void deleteThread(String url);

    /**
     * 更新线程下载进度
     */
    public void updateThread(String url, int thread_id, int finished);

    /**
     * 查询对应文件所有的线程信息
     */
    public List<ThreadInfo> getThread(String url);

    /**
     * 线程信息是否存在
     */
    public boolean isExist(String url, int thread_id);
}
