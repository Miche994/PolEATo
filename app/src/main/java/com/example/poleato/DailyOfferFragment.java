package com.example.poleato;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import com.example.poleato.ExpandableListManagement.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DailyOfferFragment extends Fragment {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataGroup;
    HashMap<String, List<String>> listDataChild;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu, container, false);

        // get the listview
        expListView = (ExpandableListView) view.findViewById(R.id.expandableList);

        // preparing list data
        prepareListData();

        Context con = this.getContext();
        listAdapter = new ExpandableListAdapter(this.getContext(), listDataGroup, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        return view;
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataGroup = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataGroup.add("Top 250");
        listDataGroup.add("Now Showing");
        listDataGroup.add("Coming Soon..");

        // Adding child data
        List<String> top250 = new ArrayList<String>();
        top250.add("The Shawshank Redemption");
        top250.add("The Godfather");
        top250.add("The Godfather: Part II");
        top250.add("Pulp Fiction");
        top250.add("The Good, the Bad and the Ugly");
        top250.add("The Dark Knight");
        top250.add("12 Angry Men");

        List<String> nowShowing = new ArrayList<String>();
        nowShowing.add("The Conjuring");
        nowShowing.add("Despicable Me 2");
        nowShowing.add("Turbo");
        nowShowing.add("Grown Ups 2");
        nowShowing.add("Red 2");
        nowShowing.add("The Wolverine");

        List<String> comingSoon = new ArrayList<String>();
        comingSoon.add("2 Guns");
        comingSoon.add("The Smurfs 2");
        comingSoon.add("The Spectacular Now");
        comingSoon.add("The Canyons");
        comingSoon.add("Europa Report");

        listDataChild.put(listDataGroup.get(0), top250); // Header, Child data
        listDataChild.put(listDataGroup.get(1), nowShowing);
        listDataChild.put(listDataGroup.get(2), comingSoon);
    }

}

