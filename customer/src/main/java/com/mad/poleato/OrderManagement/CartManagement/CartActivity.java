package com.mad.poleato.OrderManagement.CartManagement;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mad.poleato.ConnectionManager;
import com.mad.poleato.Classes.Food;
import com.mad.poleato.Interface;
import com.mad.poleato.OrderManagement.Order;
import com.mad.poleato.R;
import com.mad.poleato.TimePickerFragment;
import com.onesignal.OneSignal;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import java.util.Scanner;

public class CartActivity extends AppCompatActivity implements Interface,TimePickerDialog.OnTimeSetListener {

    private Order order;
    private boolean flag=false;
    private static TextView tvTotal;
    private static TextView tvEmptyCart;
    private CartRecyclerViewAdapter recyclerAdapter;
    private Button orderBtn;
    private RecyclerView.LayoutManager layoutManager;
    private static RecyclerView rv;
    private List<Food> foodList;
    private Toast myToast;
    private EditText time;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private ConnectionManager connectionManager;
    BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(!connectionManager.haveNetworkConnection(context))
                connectionManager.showDialog(context);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_layout);
        connectionManager = new ConnectionManager();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserID = currentUser.getUid();

        myToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);


        // OneSignal is used to send notifications between applications

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        OneSignal.setSubscription(true);

        OneSignal.sendTag("User_ID", currentUserID);

        order = (Order) getIntent().getSerializableExtra("order");

        foodList = new ArrayList<>(order.getSelectedFoods().values());

        tvTotal = (TextView) findViewById(R.id.tv_total);
        tvEmptyCart = (TextView) findViewById(R.id.empty_cart);

        time= findViewById(R.id.input_time);
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment timePicker = new TimePickerFragment();
                ((TimePickerFragment) timePicker).setListener(CartActivity.this);
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        orderBtn = (Button) findViewById(R.id.btn_placeorder);
        rv = (RecyclerView) findViewById(R.id.recycler_cart);
        rv.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        this.recyclerAdapter = new CartRecyclerViewAdapter(getApplicationContext(), foodList, order);
        rv.setAdapter(recyclerAdapter);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(getApplicationContext(), 1 );
        rv.addItemDecoration(itemDecoration);

        if(order.getSelectedFoods().isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmptyCart.setVisibility(View.VISIBLE);
        }else {
            rv.setVisibility(View.VISIBLE);
            tvEmptyCart.setVisibility(View.GONE);

        }

        computeTotal(order.getTotalPrice());
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean wrongField= false;
                if(order.getSelectedFoods().isEmpty()){
                    wrongField= true;
                    myToast.setText(getString(R.string.empty_cart));
                    myToast.show();
                }

                if(time.getText().toString().equals("")) {
                    wrongField=true;
                    myToast.setText(getString(R.string.specify_time));
                    myToast.show();
                    time.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.border_wrong_field));
                }

                if(!wrongField){
                    AlertDialog.Builder builder = new AlertDialog.Builder(CartActivity.this);
                    builder.setTitle(getString(R.string.confirm_order));
                    builder.setMessage(getString(R.string.order_proceed));
                    builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            /**
                             * Add order into restaurant DB and into customer DB (order history)
                             */
                            order.setDishes();
                            order.setTime(time.getText().toString());
                            order.setStatus(getApplicationContext().getString(R.string.new_order));
                            order.uploadOrder();

                            /**
                             * Send notification to restaurant: NEW ORDER
                             */
                            sendNotification();

                            Intent returnIntent = new Intent();
                            setResult(Activity.RESULT_OK, returnIntent);
                            flag = true;
                            finish();
                        }
                    });

                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver,filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);

    }

    private void sendNotification() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int SDK_INT = android.os.Build.VERSION.SDK_INT;
                if (SDK_INT > 8) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    String send_email;

                    //This is a Simple Logic to Send Notification different Device Programmatically....
                    send_email= order.getRestaurantID();

                    try {
                        String jsonResponse;

                        URL url = new URL("https://onesignal.com/api/v1/notifications");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setUseCaches(false);
                        con.setDoOutput(true);
                        con.setDoInput(true);

                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        con.setRequestProperty("Authorization", "Basic YjdkNzQzZWQtYTlkYy00MmIzLTg0NDUtZmQ3MDg0ODc4YmQ1");
                        con.setRequestMethod("POST");
                        String strJsonBody = "{"
                                + "\"app_id\": \"a2d0eb0d-4b93-4b96-853e-dcfe6c34778e\","

                                + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + send_email + "\"}],"

                                + "\"data\": {\"Order\": \"PolEATo\"},"
                                + "\"contents\": {\"en\": \"New order\"}"
                                + "}";


                        System.out.println("strJsonBody:\n" + strJsonBody);

                        byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                        con.setFixedLengthStreamingMode(sendBytes.length);

                        OutputStream outputStream = con.getOutputStream();
                        outputStream.write(sendBytes);

                        int httpResponse = con.getResponseCode();
                        System.out.println("httpResponse: " + httpResponse);

                        if (httpResponse >= HttpURLConnection.HTTP_OK
                                && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        } else {
                            Scanner scanner = new Scanner(con.getErrorStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        }
                        System.out.println("jsonResponse:\n" + jsonResponse);

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!flag){
            Intent resultIntent= new Intent();
            resultIntent.putExtra("old_order", order);
            setResult(Activity.RESULT_CANCELED,resultIntent);
        }
        super.onBackPressed();

    }

    @Override
    public Order getOrder() {
        return order;
    }

    public static void computeTotal(Double price){
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        String priceStr = decimalFormat.format(price).toString()+"€";
        tvTotal.setText(priceStr);

        if(price==0){
            rv.setVisibility(View.GONE);
            tvEmptyCart.setVisibility(View.VISIBLE);
        }
        else{
            rv.setVisibility(View.VISIBLE);
            tvEmptyCart.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        String hourStr;
        String minStr;

        //convert to format HH:mm
        if(hourOfDay < 10)
            hourStr = "0" + hourOfDay;
        else
            hourStr = "" + hourOfDay;

        if(minute < 10)
            minStr = "0" + minute;
        else
            minStr = "" + minute;
        time.setText(hourStr +":"+minStr);
    }
}
