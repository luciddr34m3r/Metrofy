package com.trekonnect.metrofy;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.trekonnect.metrofy.WMATA.MetroTrain;

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

/**
 * Created by cbarnard on 12/29/14.
 */
public class StationTimesFragment extends Fragment {


    static final String EXTRA_STATION = "station";
    private String mStationCode;
    private List<MetroTrain> mTrains = new ArrayList<MetroTrain>();

    public TrainsAdapter mTrainsAdapter;

    public StationTimesFragment() {
        // Required empty public constructor
    }


    public static final StationTimesFragment newInstance(String stationCode) {
        StationTimesFragment f = new StationTimesFragment();
        Bundle b1 = new Bundle();

        //TODO: Need to get this to put the metro line oject, not just the line code
        b1.putString(EXTRA_STATION, stationCode);
        f.setArguments(b1);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStationCode = getArguments() != null ? getArguments().getString(EXTRA_STATION) : "";

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_station_times, container, false);


        mTrainsAdapter = new TrainsAdapter(
                getActivity(),
                R.layout.list_item_train,
                mTrains
        );

        ListView listView = (ListView) rootView.findViewById(
                R.id.listview_station_times
        );
        listView.setAdapter(mTrainsAdapter);

        FetchTrainsTask fetchTrainsTask = new FetchTrainsTask();
        fetchTrainsTask.execute();

        return rootView;
    }

    public class TrainsAdapter extends ArrayAdapter<MetroTrain> {
        public TrainsAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public TrainsAdapter(Context context, int resource, List<MetroTrain> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.list_item_train, parent, false);
            }

            MetroTrain p = getItem(position);

            if (p != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.list_item_train_dst);
                TextView tt2 = (TextView) v.findViewById(R.id.list_item_train_time);

                if (tt1 != null) {
                    tt1.setText(p.getDestination());
                }
                if (tt2 != null) {
                    tt2.setText(p.getTime());
                }
            }
            return v;
        }
    }

    public class FetchTrainsTask extends AsyncTask<String, Void, List<MetroTrain>> {

        private static final String API_KEY = "beb00db35c294569a929a9dee4b9d89b";
        private static final String API_URI = "wmataapibeta.azure-api.net";
        private static final String API_SERVICE = "StationPrediction.svc";


        //TODO: This is a dirty hack. Need to fix the ArrayAdapter for this object.
        @Override
        protected void onPostExecute(List<MetroTrain> metroTrains) {
            super.onPostExecute(metroTrains);

            mTrains = metroTrains;

            // Dirty hack, delete this later. Should be using a custom adapter.
            mTrainsAdapter.clear();

            for(MetroTrain train : mTrains) {
                mTrainsAdapter.insert(train, mTrainsAdapter.getCount());

            }
        }

        @Override
        protected List<MetroTrain> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String trainListJSON;


            try {

                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.scheme("http")
                        .authority(API_URI)
                        .appendPath(API_SERVICE)
                        .appendPath("json")
                        .appendPath("GetPrediction")
                        .appendPath(mStationCode)
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

                trainListJSON = buffer.toString();

                Log.v("JSON output", trainListJSON);

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
                return getTrainListFromJson(trainListJSON);
            } catch (JSONException e) {
                Log.v("Getting data from json", e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        private List<MetroTrain> getTrainListFromJson(String trainListJSON) throws JSONException {

            final String TRAINS = "Trains";
            final String DESTINATION_NAME = "DestinationName";
            final String MIN = "Min";
            final String DESTINATION_CODE = "Destination";
            final String CAR = "Car";

            List<MetroTrain> trains = new ArrayList<MetroTrain>();

            JSONObject queryJson = new JSONObject(trainListJSON);
            JSONArray trainArray = queryJson.getJSONArray(TRAINS);

            for(int i = 0; i<trainArray.length(); i++) {
                JSONObject train = trainArray.getJSONObject(i);


                trains.add(
                        new MetroTrain(
                                train.getString(DESTINATION_NAME),
                                train.getString(DESTINATION_CODE),
                                train.getString(CAR),
                                train.getString(MIN)
                        )
                );
            }

            return trains;
        }
    }

}
