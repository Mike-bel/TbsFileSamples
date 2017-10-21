package com.yifeng.sample.tbs;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import com.tencent.smtt.sdk.TbsReaderView;
import com.tencent.smtt.sdk.TbsReaderView.ReaderCallback;
import java.io.File;

public class MainActivity extends AppCompatActivity implements ReaderCallback {

  private TbsReaderView mTbsReaderView;
  private Button mDownloadBtn;

  private DownloadManager mDownloadManager;
  private long mRequestId;
  private DownloadObserver mDownloadObserver;
  private String mFileUrl = "http://www.beijing.gov.cn/zhuanti/ggfw/htsfwbxzzt/shxfl/fw/P020150720516332194302.doc";
  private String mFileName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mTbsReaderView = new TbsReaderView(this, this);
    mDownloadBtn = (Button) findViewById(R.id.btn_download);
    RelativeLayout rootRl = (RelativeLayout) findViewById(R.id.rl_root);
    rootRl.addView(mTbsReaderView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    mFileName = parseName(mFileUrl);
    if (isLocalExist()) {
      mDownloadBtn.setText("打开文件");
    }
  }

  public void onClickDownload(View v) {
    if (isLocalExist()) {
      mDownloadBtn.setVisibility(View.GONE);
      displayFile();
    } else {
      startDownload();
    }
  }

  private void displayFile() {
    Bundle bundle = new Bundle();
    bundle.putString("filePath", getLocalFile().getPath());
    bundle.putString("tempPath", Environment.getExternalStorageDirectory().getPath());
    boolean result = mTbsReaderView.preOpen(parseFormat(mFileName), false);
    if (result) {
      mTbsReaderView.openFile(bundle);
    }
  }

  private String parseFormat(String fileName) {
    return fileName.substring(fileName.lastIndexOf(".") + 1);
  }

  private String parseName(String url) {
    String fileName = null;
    try {
      fileName = url.substring(url.lastIndexOf("/") + 1);
    } finally {
      if (TextUtils.isEmpty(fileName)) {
        fileName = String.valueOf(System.currentTimeMillis());
      }
    }
    return fileName;
  }

  private boolean isLocalExist() {
    return getLocalFile().exists();
  }

  private File getLocalFile() {
    return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mFileName);
  }

  private void startDownload() {
    mDownloadObserver = new DownloadObserver(new Handler());
    getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, mDownloadObserver);

    mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mFileUrl));
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFileName);
    request.allowScanningByMediaScanner();
    request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
    mRequestId = mDownloadManager.enqueue(request);
  }

  private void queryDownloadStatus() {
    DownloadManager.Query query = new DownloadManager.Query().setFilterById(mRequestId);
    Cursor cursor = null;
    try {
      cursor = mDownloadManager.query(query);
      if (cursor != null && cursor.moveToFirst()) {
        //已经下载的字节数
        int currentBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        //总需下载的字节数
        int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        //状态所在的列索引
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        Log.i("downloadUpdate: ", currentBytes + " " + totalBytes + " " + status);
        mDownloadBtn.setText("正在下载：" + currentBytes + "/" + totalBytes);
        if (DownloadManager.STATUS_SUCCESSFUL == status && mDownloadBtn.getVisibility() == View.VISIBLE) {
          mDownloadBtn.setVisibility(View.GONE);
          mDownloadBtn.performClick();
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public void onCallBackAction(Integer integer, Object o, Object o1) {

  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mTbsReaderView.onStop();
    if (mDownloadObserver != null) {
      getContentResolver().unregisterContentObserver(mDownloadObserver);
    }
  }

  private class DownloadObserver extends ContentObserver {

    private DownloadObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
      Log.i("downloadUpdate: ", "onChange(boolean selfChange, Uri uri)");
      queryDownloadStatus();
    }
  }
}
