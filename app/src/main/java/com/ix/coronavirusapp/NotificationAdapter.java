package com.ix.coronavirusapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NotificationAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;

    List<Notification> list;

    public NotificationAdapter(Context context, List<Notification> objects) {
        this.list = objects;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.notification_listview,null);

        TextView date = (TextView) convertView.findViewById(R.id.date);
        TextView detail = (TextView) convertView.findViewById(R.id.detail);
        TextView time = (TextView) convertView.findViewById(R.id.time);

        date.setText(list.get(position).getDate());
        time.setText(list.get(position).getTime());
        detail.setText(list.get(position).getText());
        return convertView;
    }
}

