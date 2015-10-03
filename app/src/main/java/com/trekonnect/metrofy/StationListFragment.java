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

import com.trekonnect.metrofy.WMATA.MetroStation;

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


public class StationListFragment extends Fragment {

    //private List<MetroStation> mStations;
    static final String EXTRA_LINE = "line";
    private String mMetroLine;
    private List<MetroStation> mStations = new ArrayList<MetroStation>();

    private String[] stationArray;

    public StationsAdapter mStationsAdapter;

    public StationListFragment() {
        // Required empty public constructor
    }


    public static final StationListFragment newInstance(String lineCode) {
        StationListFragment f = new StationListFragment();
        Bundle b1 = new Bundle();

        //TODO: Need to get this to put the metro line oject, not just the line code
        b1.putString(EXTRA_LINE, lineCode);
        f.setArguments(b1);

        return f;
    }

//    public static final StationListFragment newInstance(MetroLine line) {
//        StationListFragment f = new StationListFragment();
//        Bundle b1 = new Bundle();
//
//        //TODO: Need to get this to put the metro line oject, not just the line code
//        b1.putString(EXTRA_LINE, line.getLineCode());
//        f.setArguments(b1);
//
//        return f;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String line = getArguments() != null ? getArguments().getString(EXTRA_LINE) : "";

        mMetroLine = line;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_list, container, false);

        mStationsAdapter = new StationsAdapter(
                getActivity(),
                R.layout.list_item_station,
                mStations
        );

        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_station_list
        );
        listView.setAdapter(mStationsAdapter);

        FetchStationsTask fetchStationsTask = new FetchStationsTask();
        fetchStationsTask.execute();



        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(

        ) {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MetroStation clickedStation;
                Bundle b = new Bundle();

//                StationTimesFragment stationTimes = new StationTimesFragment();
//
//                FragmentManager fm = getFragmentManager();
//                FragmentTransaction ft = fm.beginTransaction();
//
//                ft.replace(R.id.container, stationTimes);
//
//                ft.addToBackStack(null);
//                ft.commit();

                clickedStation = mStationsAdapter.getItem(position);

                b.putString("station", clickedStation.getStationCode());

                Intent timesIntent = new Intent(getActivity(), StationTimesActivity.class);
                timesIntent.putExtras(b);

                startActivity(timesIntent);

            }
        });

        return rootView;
    }

    public class StationsAdapter extends ArrayAdapter<MetroStation> {

        public StationsAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public StationsAdapter(Context context, int resource, List<MetroStation> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.list_item_station, null);
            }

            MetroStation p = getItem(position);

            if (p != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.list_item_station_textview);

                if (tt1 != null) {
                    tt1.setText(p.getStationName());
                }
            }
            return v;
        }
    }

    public class FetchStationsTask extends AsyncTask<String, Void, List<MetroStation>> {

        private static final String API_KEY = "beb00db35c294569a929a9dee4b9d89b";
        private static final String API_URI = "wmataapibeta.azure-api.net";
        private static final String API_SERVICE = "Rail.svc";


        //TODO: This is a dirty hack. Need to fix the ArrayAdapter for this object.
        @Override
        protected void onPostExecute(List<MetroStation> metroStations) {
            super.onPostExecute(metroStations);

            mStations = metroStations;

            // Dirty hack, delete this later. Should be using a custom adapter.
            mStationsAdapter.clear();

            for(MetroStation station : mStations) {
                mStationsAdapter.insert(station, mStationsAdapter.getCount());

            }
        }

        @Override
        protected List<MetroStation> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String stationListJSONString;


            try {

                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.scheme("http")
                        .authority(API_URI)
                        .appendPath(API_SERVICE)
                        .appendPath("json")
                        .appendPath("jStations")
                        .appendQueryParameter("LineCode", mMetroLine)
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
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                stationListJSONString = buffer.toString();

                Log.v("JSON output", stationListJSONString);

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
                return getStationListFromJson(stationListJSONString);
            } catch (JSONException e) {
                Log.v("Getting data from json", e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        private List<MetroStation> getStationListFromJson(String stationListJSON) throws JSONException {

            final String STATION_DISPLAY_NAME = "Name";
            final String STATION_CODE = "Code";
            final String WMATA_STATIONS = "Stations";

            List<MetroStation> stations = new ArrayList<MetroStation>();

            JSONObject queryJson = new JSONObject(stationListJSON);
            JSONArray stationArray = queryJson.getJSONArray(WMATA_STATIONS);

            for(int i = 0; i<stationArray.length(); i++) {
                JSONObject stationDetails = stationArray.getJSONObject(i);

                stations.add(
                        new MetroStation(
                                stationDetails.getString(STATION_DISPLAY_NAME),
                                stationDetails.getString(STATION_CODE)
                        )
                );
            }

            return stations;
        }
    }



}
