package com.fsoft.vktest.ViewsLayer;

/**
 * Dr. Failov on 11.11.2014.
 */
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LogFragment extends Fragment {
    ConsoleView consoleView = null;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(MainActivity.consoleView == null)
            MainActivity.consoleView = new ConsoleView(getActivity());
        return consoleView = MainActivity.consoleView;
    }

    @Override
    public void onResume() {
        super.onResume();
        consoleView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        consoleView.setVisibility(View.INVISIBLE);
        super.onPause();
    }
}