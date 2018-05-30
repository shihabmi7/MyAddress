package fantoos.myaddress;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final long DELAY = 5000;
    private static final float ZOOM_LEVEL = 15;
    private static final String ADDRESS_NOT_FOUND = "404";
    private GoogleMap mMap;
    private Button buttonStart, buttonStop;
    private String TAG = "MapsActivity";

    private TextView text_lat_lng, text_result;
    private ProgressDialog mProgressDialog;

    boolean isWorking = false;
    String address = "";
    private TextView text_save_status;

    Handler handler = new Handler();
//    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        text_lat_lng = findViewById(R.id.text_lat_lng);
        text_result = findViewById(R.id.text_result);
        text_save_status = findViewById(R.id.text_save_status);


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Loading...");


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onClick(View v) {
        if (v == buttonStart) {

            handler.post(runnable);
            buttonStop.setAlpha(.1f);

        } else if (v == buttonStop) {

            handler.removeCallbacks(runnable);
            buttonStop.setAlpha(1f);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            if (Connectivity.isConnected(getApplicationContext())) {
                volleyStringRequest(Constants.GET_COORDINATE);
                handler.postDelayed(runnable, DELAY);
            } else {
                Toast.makeText(getApplicationContext(), "No internet connection...", Toast.LENGTH_LONG).show();
            }
        }
    };

    //int id_;
    double latitude;
    double longitude;

    public void volleyStringRequest(final String url) {
        showProgressDialog();
        final StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {

            @Override
            public void onResponse(final String response) {
                    /*{
                        "code": 200,
                            "success": true,
                            "message": "lat lng given",
                            "data": {
                        "id": 1,
                                "lat": 23.657703,
                                "lng": 90.457097
                    }
                    }*/

                if (response.length() > 0) {


                    try {
                        JSONObject jsonObj = new JSONObject(response);
                        // boolean status = jsonObj.isNull("success");

                        if (!jsonObj.isNull("success")) {
                            JSONObject dataObj = jsonObj.getJSONObject("data");
                            int id_ = dataObj.getInt("id");
                            String latitude = dataObj.getString("lat");
                            String longitude = dataObj.getString("lng");

                            text_lat_lng.setText(id_ + " " + latitude + " " + longitude);
                            String NEW_URL = Constants.FADAO + latitude + "/" + longitude;
                            Log.d(TAG, "volleyStringRequest:" + NEW_URL);

                            LatLng currentlatLong = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                            if (marker != null) {
                                marker.setPosition(currentlatLong);
                            } else {

                                marker = mMap.addMarker(new MarkerOptions().position(currentlatLong));
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlatLong, ZOOM_LEVEL));

                            getAddressRequest(NEW_URL, "" + id_, "" + latitude, "" + longitude);
                            text_save_status.setText("");
                            text_result.setText("");

                        } else {
                            text_lat_lng.setText("error");
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "volleyStringRequest" + response + "Empty Response");
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d(TAG, "Error: " + error.toString());

            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                //params.put("param_one", "");
                //params.put("param_two", "");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //LogMe.d("acc::", "token");
                params.put("Authorization", "E72DD2E");

                return params;
            }
        };
        App.getApp().addToRequestQueue(request, TAG);
    }

    Marker marker;

    public void getAddressRequest(final String url, final String id_, final String latitude, final String longitude) {

        final StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                    /*{
                        "address": "Tahsin Tahan Dental Care, 155, Modhu Bazar Goli, East Rayer Bazar, 1209"
                    }*/

                hideProgressDialog();
                if (response.length() > 0) {

                    Log.d(TAG, "volleyStringRequest" + response);
                    try {
                        JSONObject jsonObj = new JSONObject(response);
                        String address = jsonObj.getString("address");
                        text_result.setText("");
                        text_result.setText(address);

                        setAddressRequestToServerBack(Constants.POST_ADDRESS, id_, latitude, longitude, address);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                } else {
                    Log.d(TAG, "Fada Empty Response" + response + "Empty Response");
                    text_result.setText("Empty Response");
                    setAddressRequestToServerBack(Constants.POST_ADDRESS, id_, latitude, longitude, ADDRESS_NOT_FOUND);
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d(TAG, "Error: " + error.toString());

            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                //params.put("param_one", "");
                //params.put("param_two", "");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //LogMe.d("acc::", "token");
                params.put("Authorization", "Bearer 6Eq0MuXyvHHuIcjF8hXYKdtukwk2Gzpj1k64vj5d");

                return params;
            }
        };
        App.getApp().addToRequestQueue(request, TAG);
    }

    public void setAddressRequestToServerBack(final String url, final String id_, final String latitude, final String longitude, final String address) {


        final StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                    /*{
                        "code": 200,
                            "success": true,
                            "message": "saved",
                            "data": null
                    }*/
                try {

                    JSONObject jsonObj = new JSONObject(response);
                    boolean status = jsonObj.getBoolean("success");
                    if (status) {


                        text_save_status.setText("saved");

                    } else {

                        text_save_status.setText("");
                        text_save_status.setText("failed..");
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "volleyStringRequest" + response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d(TAG, "Error: " + error.toString());

            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                //params.put("param_one", "");
                //params.put("param_two", "");
                params.put("id", "" + id_);
                params.put("lat", "" + latitude);
                params.put("lng", "" + longitude);
                params.put("address", "" + address);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //LogMe.d("acc::", "token");
                params.put("Authorization", "E72DD2E");
                return params;
            }
        };
        App.getApp().addToRequestQueue(request, TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void showProgressDialog() {

        if (mProgressDialog != null && !mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    public void hideProgressDialog() {

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}
