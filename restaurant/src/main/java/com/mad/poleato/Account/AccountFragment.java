package com.mad.poleato.Account;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mad.poleato.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AccountFragment extends Fragment {

    private Toast myToast;

    private Map<String, TextView> tvFields;

    private FloatingActionButton buttEdit;
    private ImageView profileImage;

    private ProgressDialog progressDialog;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progressDialog.dismiss();
        }
    };


    String localeShort;

    String currentUserID;
    private FirebaseAuth mAuth;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
        myToast = Toast.makeText(getActivity(), "", Toast.LENGTH_LONG);

        //download Type base on the current active Locale
        String locale = Locale.getDefault().toString();
        Log.d("matte", "LOCALE: "+locale);
        localeShort = locale.substring(0, 2);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUserID = currentUser.getUid();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_frag_layout, container, false);

        // Retrieve all fields (restaurant details) in the xml file
        tvFields = new HashMap<>();
        tvFields.put("Name", (TextView)view.findViewById(R.id.tvNameField));
        tvFields.put("Type", (TextView)view.findViewById(R.id.tvTypeField));
        tvFields.put("Info", (TextView)view.findViewById(R.id.tvInfoField));
        tvFields.put("Open", (TextView)view.findViewById(R.id.tvOpenField));
        tvFields.put("Address", (TextView)view.findViewById(R.id.tvAddressField));
        tvFields.put("Email", (TextView)view.findViewById(R.id.tvEmailField));
        tvFields.put("Phone", (TextView)view.findViewById(R.id.tvPhoneField));
        tvFields.put("DeliveryCost", (TextView)view.findViewById(R.id.tvDeliveryCostField));
        tvFields.put("IsActive", (TextView)view.findViewById(R.id.tvStatusField));
        tvFields.put("PriceRange", (TextView)view.findViewById(R.id.tvPriceRangeField));

        profileImage = view.findViewById(R.id.ivBackground);

        // Button to edit the restaurant details
        buttEdit = view.findViewById(R.id.buttEdit);
        buttEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * GO TO EDIT_PROFILE_FRAGMENT
                 */
                Navigation.findNavController(v).navigate(R.id.action_account_id_to_editProfile_id);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //fill the views fields
        if(getActivity() != null)
            progressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.loading));

        //start a new thread to process job
        fillFields();

    }

    public void fillFields() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("restaurants/"+ currentUserID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // it is set to the first record (restaurant)
                // when the sign in and log in procedures will be handled, it will be the proper one
                if (dataSnapshot.exists()) {
                    // dataSnapshot is the "issue" node with all children

                    if(dataSnapshot.hasChild("DeliveryCost") &&
                            dataSnapshot.hasChild("IsActive") &&
                            //dataSnapshot.hasChild("PriceRange") &&
                            dataSnapshot.hasChild("Type") &&
                            dataSnapshot.child("Type").hasChild(localeShort))
                    {
                        for(DataSnapshot snap : dataSnapshot.getChildren()){
                            if(tvFields.containsKey(snap.getKey())){
                                if(snap.getKey().equals("DeliveryCost")){
                                    //DecimalFormat decimalFormat = new DecimalFormat("#0.00"); //two decimal
                                    //String priceStr = decimalFormat.format(Double.parseDouble(snap.getValue().toString()));
                                    tvFields.get(snap.getKey()).setText(snap.getValue().toString()+"€");
                                }
                                else if(snap.getKey().equals("IsActive") && getActivity() != null){
                                    if((Boolean)snap.getValue())
                                        tvFields.get(snap.getKey()).setText(getString(R.string.active_status));
                                    else
                                        tvFields.get(snap.getKey()).setText(getString(R.string.inactive_status));
                                }
                                else if(snap.getKey().equals("PriceRange")){
                                    //translate price range value into a $ string
                                    int count = Integer.parseInt(snap.getValue().toString());
                                    String s = "";
                                    for(int idx = 0; idx < count; idx ++)
                                        s += "$";
                                    tvFields.get(snap.getKey()).setText(s);
                                }
                                else if(snap.getKey().equals("Type"))
                                    tvFields.get(snap.getKey()).setText(snap.child(localeShort).getValue().toString());
                                else
                                    tvFields.get(snap.getKey()).setText(snap.getValue().toString());
                            }
                        } //for end

                    } //end if
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("matte", "onCancelled | ERROR: " + databaseError.getDetails() +
                        " | MESSAGE: " + databaseError.getMessage());
                myToast.setText(databaseError.getMessage().toString());
                myToast.show();
            }
        });

        //Download the profile pic
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference photoReference= storageReference.child(currentUserID +"/ProfileImage/img.jpg");

        final long ONE_MEGABYTE = 1024 * 1024;
        photoReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileImage.setImageBitmap(bmp);
                //send message to main thread
                handler.sendEmptyMessage(0);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("matte", "No image found. Default img setting");
                //set predefined image
                profileImage.setImageResource(R.drawable.plate_fork);
                //send message to main thread
                handler.sendEmptyMessage(0);
            }
        });

    }

}

