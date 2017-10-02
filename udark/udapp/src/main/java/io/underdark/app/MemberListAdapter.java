package io.underdark.app;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;

import java.util.List;


public class MemberListAdapter extends ArrayAdapter<Button> {
    private List<Button> items;
    private Context context;
    private int layoutResourceId;

    public MemberListAdapter(Context context, int layoutResourceId, List<Button> items){
        super(context, layoutResourceId, items);
        Log.v("MemberListAdapter","after super");
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.items = items;
    }
}
