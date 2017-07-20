package tk.rabidbeaver.dashcam;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static tk.rabidbeaver.dashcam.DashCamService.mRPiAddress;

public class LogsFragment extends Fragment {
    private View rootView;
    private SwipeRefreshLayout pullrefresher;
    private TextView logView;
    private String response;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("LogsFragment", "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_logs, container, false);
        return rootView;
    }

    private void loadLogs(){
        new Thread(new Runnable() {
            public void run() {
                response = "";
                HttpURLConnection urlConnection;
                try {
                    URL url = new URL("http://" + mRPiAddress + ":8888/crashlog");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(2000);
                    urlConnection.setReadTimeout(2000);
                    urlConnection.connect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }

                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Log.d("CHECK", response);
                urlConnection.disconnect();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        logView.setText(response);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        pullrefresher = (SwipeRefreshLayout) rootView.findViewById(R.id.pullrefresher);
        pullrefresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadLogs();
                if (pullrefresher != null) pullrefresher.setRefreshing(false);
            }
        });
        logView = (TextView) rootView.findViewById(R.id.logview);
        logView.setOnLongClickListener(new TextView.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                DashCamService.uploadLogs(getContext(), Constants.LOG_ID.CRASH_LOG);
                return false;
            }
        });

        if (pullrefresher != null) pullrefresher.setRefreshing(false);
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        Log.d("LogsFragment", "setUserVisibleHint: "+Boolean.toString(visible));
        if (visible){
            loadLogs();
            if (pullrefresher != null) pullrefresher.setRefreshing(false);
        }
    }
}
