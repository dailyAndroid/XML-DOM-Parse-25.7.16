package com.example.hwhong.xmldomparse;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements ResultsCallBack {

    PlaceholderFragment taskFragment;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //if it is the first time created
        if (savedInstanceState == null) {
            taskFragment = new PlaceholderFragment();
            getSupportFragmentManager().beginTransaction().
                    add(taskFragment, "Fragment").commit();
        } else {
            //if it is not the first time being created
            taskFragment = (PlaceholderFragment)  getSupportFragmentManager().findFragmentByTag("Fragment");
        }
        taskFragment.startDownload();

        listView = (ListView) findViewById(R.id.listView);
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onPostExecute(ArrayList<HashMap<String, String>> list) {
        listView.setAdapter(new MyAdapter(getApplicationContext(), list));
    }

    public static class PlaceholderFragment extends Fragment {

        TechCrunchTask download;
        ResultsCallBack resultsCallBack;

        public PlaceholderFragment() {

        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            resultsCallBack = (ResultsCallBack) activity;

            if (download != null) {
                download.onAttach(resultsCallBack);
            }
        }

        //As the name states, this is called after the Activity's onCreate() has completed.
        // It is called after onCreateView(), and is mainly used for final initialisations
        // (for example, modifying UI elements).

        //Called when the fragment's activity has been created and this
        // fragment's view hierarchy instantiated.
        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            //making sure that the fragment never gets destroyed
            setRetainInstance(true);
        }

        public void startDownload() {
            if (download != null) {
                download.cancel(true);
            } else {
                download = new TechCrunchTask(resultsCallBack);
                download.execute();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            resultsCallBack = null;
            if (download != null) {
                download.onDetach();
            }
        }
    }

    public static class TechCrunchTask extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>> {

        ResultsCallBack resultsCallBack = null;

        public TechCrunchTask(ResultsCallBack resultsCallBack) {
            this.resultsCallBack = resultsCallBack;
        }

        public void onAttach(ResultsCallBack resultsCallBack) {
            this.resultsCallBack = resultsCallBack;
        }

        public void onDetach() {
            resultsCallBack = null;
        }

        @Override
        protected void onPreExecute() {
            if(resultsCallBack != null) {
                resultsCallBack.onPreExecute();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> hashMaps) {
            if(resultsCallBack != null) {
                resultsCallBack.onPostExecute(hashMaps);
            }
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(Void... voids) {
            String downloadUrl = "http://feeds.feedburner.com/techcrunch/android?format=xml";
            ArrayList<HashMap<String, String>> hashArray = new ArrayList<>();
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream inputStream = urlConnection.getInputStream();

                hashArray = parseXML(inputStream);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return hashArray;
        }

        public ArrayList<HashMap<String, String>> parseXML(InputStream inputStream) {
            //a singleton class, meaning that there is only one object existing
            //therefore dont use the new keyword
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document xmlDoc = documentBuilder.parse(inputStream);

                //returns the root element of this document
                Element rootElem = xmlDoc.getDocumentElement();

                Log.e("hey", "" + rootElem.getTagName());

                NodeList list = rootElem.getElementsByTagName("item");
                NodeList children = null;
                Node currentItem = null;
                Node currentChild = null;
                NamedNodeMap mediaThumbnail;
                Node mediaAttribute = null;
                int count = 0;
                ArrayList<HashMap<String, String>> hashArray = new ArrayList<>();
                HashMap<String, String> hashmap = null;
                for (int i = 0; i < list.getLength(); i++) {
                    currentItem = list.item(i);
                    children = currentItem.getChildNodes();

                    hashmap = new HashMap<>();

                    for (int x = 0; x < children.getLength(); x++) {
                        currentChild = children.item(x);

                        if (currentChild.getNodeName().equalsIgnoreCase("title")) {
                            Log.d("hey2", currentChild.getTextContent());
                            hashmap.put("title", currentChild.getTextContent());
                        }
                        if (currentChild.getNodeName().equalsIgnoreCase("pubDate")) {
                            Log.d("hey2", currentChild.getTextContent());
                            hashmap.put("pubDate", currentChild.getTextContent());
                        }
                        if (currentChild.getNodeName().equalsIgnoreCase("description")) {
                            Log.d("hey2", currentChild.getTextContent());
                            hashmap.put("description", currentChild.getTextContent());
                        }
                        if (currentChild.getNodeName().equalsIgnoreCase("media:thumbnail")) {
                            count++;
                            if (count == 2) {
                                hashmap.put("imageURL", currentChild.getAttributes().
                                        item(0).getTextContent());
                            }
                        }
                    }
                    if (hashmap != null && !hashmap.isEmpty()) {
                        hashArray.add(hashmap);
                    }
                    count = 0;
                }
                return hashArray;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    class MyAdapter extends BaseAdapter {

        ArrayList<HashMap<String, String>> hashMaps;
        Context context;
        LayoutInflater inflater;

        public MyAdapter(Context context, ArrayList<HashMap<String, String>> hashMaps) {
            this.hashMaps = hashMaps;
            this.context = context;
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return hashMaps.size();
        }

        @Override
        public Object getItem(int i) {
            return hashMaps.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder viewHolder = null;
            if (row == null) {
                row = inflater.inflate(R.layout.row, viewGroup, false);
                viewHolder = new ViewHolder(row);
                row.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) row.getTag();
            }

            HashMap<String, String> hashMap = hashMaps.get(i);
            viewHolder.title.setText(hashMap.get("title"));
            viewHolder.date.setText(hashMap.get("pubDate"));
            viewHolder.description.setText(hashMap.get("description"));
            //viewHolder.imageView.setImageURI(getImageBitmap(hashMaps.get("imageURL")));

            return row;
        }
    }

    class ViewHolder {
        TextView title, date, description;
        ImageView imageView;

        public ViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.title);
            date = (TextView) view.findViewById(R.id.date);
            description = (TextView) view.findViewById(R.id.description);
            imageView = (ImageView) view.findViewById(R.id.imageView);
        }
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e("Error getting bitmap", "error");
        }
        return bm;
    }
}

