package com.mad.poleato.Account;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.Navigation;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mad.poleato.R;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;


public class AccountFragment extends Fragment {


    private Map<String, TextView> tvFields;

    private FloatingActionButton buttEdit;
    private ImageView imageBackground;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
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

        imageBackground = view.findViewById(R.id.ivBackground);

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
        fillFields();
    }

    public void fillFields() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("restaurants");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot issue = dataSnapshot.child("R00");
                // it is setted to the first record (restaurant)
                // when the sign in and log in procedures will be handled, it will be the proper one
                if (dataSnapshot.exists()) {
                    // dataSnapshot is the "issue" node with all children
                    for(DataSnapshot snap : issue.getChildren()){
                        if(tvFields.containsKey(snap.getKey())){
                            if(snap.getKey().equals("DeliveryCost")){
                                DecimalFormat decimalFormat = new DecimalFormat("#.00"); //two decimal
                                String priceStr = decimalFormat.format(Double.parseDouble(snap.getValue().toString()));
                                tvFields.get(snap.getKey()).setText(priceStr+"€");
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
                            else
                                tvFields.get(snap.getKey()).setText(snap.getValue().toString());
                        }
                    } //for end
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("matte", "onCancelled | ERROR: " + databaseError.getDetails() +
                        " | MESSAGE: " + databaseError.getMessage());
                Toast.makeText(getContext(), databaseError.getMessage().toString(), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

//        final ScrollView mScrollView = getView().findViewById(R.id.mainScrollView);
//        //saving scrollView position when rotate the screen
//        outState.putIntArray("ARTICLE_SCROLL_POSITION",
//                new int[]{ mScrollView.getScrollX(), mScrollView.getScrollY()});
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ScrollView mScrollView = getView().findViewById(R.id.mainScrollView);
        //restoring scrollview position
//        final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
//        if (position != null) {
//            mScrollView.post(new Runnable() {
//                public void run() {
//                    mScrollView.scrollTo(position[0], position[1]);
//                }
//            });
//        }

    }
}

