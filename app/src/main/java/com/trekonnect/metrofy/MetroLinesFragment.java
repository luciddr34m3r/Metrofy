package com.trekonnect.metrofy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.trekonnect.metrofy.WMATA.MetroLine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MetroLinesFragment extends Fragment {


    public LinesAdapter mLinesAdapter;
    private List<MetroLine> mLines = new ArrayList<MetroLine>();
    private boolean updated;

    private ArrayList<MetroLine> lineArray;

    public MetroLinesFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updated = false;

    }

    public class LinesAdapter extends ArrayAdapter<MetroLine> {

        public LinesAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public LinesAdapter(Context context, int resource, List<MetroLine> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.list_item_line, null);
            }

            MetroLine p = getItem(position);

            if (p != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.list_item_line_textview);

                if (tt1 != null) {
                    tt1.setText(p.getDisplayName());
                }
            }
            return v;
        }
    }

    public class FetchLinesTask extends AsyncTask<String, Void, List<MetroLine>> {

        private static final String API_KEY = "beb00db35c294569a929a9dee4b9d89b";
        private static final String API_URI = "wmataapibeta.azure-api.net";
        private static final String API_SERVICE = "Rail.svc";


        //TODO: This is a dirty hack. Need to fix the ArrayAdapter for this object.
        @Override
        protected void onPostExecute(List<MetroLine> metroLines) {
            super.onPostExecute(metroLines);

            mLines = metroLines;

            updated = true;
            Log.v("UPDATING", "Updating");
            mLinesAdapter.clear();
            for(MetroLine line : mLines) {
                mLinesAdapter.insert(line, mLinesAdapter.getCount());
            }

        }

        @Override
        protected List<MetroLine> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String lineListJSONString;


            try {

                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.scheme("http")
                        .authority(API_URI)
                        .appendPath(API_SERVICE)
                        .appendPath("json")
                        .appendPath("jLines")
                        .appendQueryParameter("api_key", API_KEY);

                URL url = new URL(uriBuilder.toString());

                Log.v("url", uriBuilder.toString());

                // Create the request to WMATA, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                lineListJSONString = buffer.toString();

                Log.v("JSON output", lineListJSONString);

            } catch(IOException e) {
                Log.v("API QUERY FAILED: ", e.getMessage(), e);
                e.printStackTrace();
                return null;

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("Connect AsyncTask", "Error closing stream", e);
                    }
                }
            }

            try {
                return getLineListFromJson(lineListJSONString);
            } catch (JSONException e) {
                Log.v("Getting data from json", e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        private List<MetroLine> getLineListFromJson(String lineListJSON) throws JSONException {

            final String LINE_DISPLAY_NAME = "DisplayName";
            final String LINE_CODE = "LineCode";
            final String WMATA_LINES = "Lines";

            List<MetroLine> lines = new ArrayList<MetroLine>();

            JSONObject queryJson = new JSONObject(lineListJSON);
            JSONArray lineArray = queryJson.getJSONArray(WMATA_LINES);

            for(int i = 0; i<lineArray.length(); i++) {
                JSONObject lineDetails = lineArray.getJSONObject(i);

                lines.add(
                        new MetroLine(
                                lineDetails.getString(LINE_DISPLAY_NAME),
                                lineDetails.getString(LINE_CODE)
                        )
                );
            }

            return lines;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_metro_lines, container, false);


        mLinesAdapter = new LinesAdapter(
                this.getActivity(),
                R.layout.fragment_metro_lines,
                mLines
        );

        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_metro_lines
        );
        //listView.setEmptyView(new TextView(this.getActivity()));
        listView.setAdapter(mLinesAdapter);

        FetchLinesTask linesTask = new FetchLinesTask();
        linesTask.execute();


        // TODO: This function is not safe. Could be null. Adapter issue.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(

        ) {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String line = mLinesAdapter.getItem(position).getDisplayName();
                MetroLine clickedLine = null;
                Bundle b = new Bundle();

                clickedLine = mLinesAdapter.getItem(position);

//                for(int i=0; i<mLines.size(); i++) {
//                    if (mLines.get(i).getDisplayName().equals(line)) {
//                        clickedLine = mLines.get(i);
//                        break;
//                    }
//                }


//                StationListFragment stationList = StationListFragment.newInstance(clickedLine);
//
//                FragmentManager fm = getFragmentManager();
//                FragmentTransaction ft = fm.beginTransaction();
//
//                ft.replace(
//                        R.id.container,
//                        stationList
//                );
//
//                ft.addToBackStack(null);
//                ft.commit();

                Intent timesIntent = new Intent(getActivity(), StationListActivity.class);

                b.putString("line", clickedLine.getLineCode());
                timesIntent.putExtras(b);

                startActivity(timesIntent);

            }
        });

        return rootView;
    }


}
