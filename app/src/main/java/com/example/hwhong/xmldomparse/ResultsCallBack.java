package com.example.hwhong.xmldomparse;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hwhong on 7/24/16.
 */
public interface ResultsCallBack {
    public void onPreExecute();
    public void onPostExecute(ArrayList<HashMap<String, String>> list);
}
