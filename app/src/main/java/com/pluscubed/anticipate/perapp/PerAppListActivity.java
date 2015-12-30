package com.pluscubed.anticipate.perapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pluscubed.anticipate.R;
import com.pluscubed.anticipate.transition.FabDialogMorphSetup;
import com.pluscubed.anticipate.util.PrefUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class PerAppListActivity extends AppCompatActivity {

    public static final String EXTRA_ADDED = "com.pluscubed.anticipate.ADDED_APP";
    //static final int PAYLOAD_ICON = 32;
    public static final int REQUEST_ADD_APP = 1001;

    public static final String DB = "Anticipate";
    public static final String TABLE_BLACKLISTED_APPS = "BlacklistedApps";
    public static final String TABLE_WHITELISTED_APPS = "WhitelistedApps";

    ProgressBar mProgressBar;

    List<AppInfo> mPerAppList;
    AppAdapter mAdapter;

    boolean mBlacklistMode;

    FloatingActionButton mFab;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_ADD_APP) {
            mPerAppList.add((AppInfo) data.getSerializableExtra(EXTRA_ADDED));

            Collections.sort(mPerAppList);

            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perapplist);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        mPerAppList = new ArrayList<>();
        mBlacklistMode = PrefUtils.isBlacklistMode(this);

        setTitle(mBlacklistMode ? getString(R.string.blacklisted_apps) : getString(R.string.whitelisted_apps));


        RecyclerView view = (RecyclerView) findViewById(R.id.recyclerview);
        mAdapter = new AppAdapter();
        view.setAdapter(mAdapter);
        view.setLayoutManager(new LinearLayoutManager(this));

        final View mockFab = findViewById(R.id.view_mock_fab);

        mFab = (FloatingActionButton) findViewById(R.id.fab_add_perapp);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PerAppListActivity.this, AddAppDialogActivity.class);

                intent.putExtra(FabDialogMorphSetup.EXTRA_SHARED_ELEMENT_START_COLOR,
                        ContextCompat.getColor(PerAppListActivity.this, R.color.colorAccent));
                Bundle options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(PerAppListActivity.this, mockFab, getString(R.string.transition_fab_add)).toBundle();
                startActivityForResult(intent, REQUEST_ADD_APP, options);
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        updateList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateList() {
        mProgressBar.setVisibility(View.VISIBLE);
        DbUtil.getPerAppListApps(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppInfo>>() {
                    @Override
                    public void onSuccess(List<AppInfo> newList) {
                        mPerAppList = newList;

                        mProgressBar.setVisibility(View.GONE);

                        //TODO: Calling this while the view hasn't been laid out causes jank
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

        public AppAdapter() {
            super();
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final AppInfo appInfo = mPerAppList.get(position);

            Glide.with(PerAppListActivity.this)
                    .load(appInfo)
                    .crossFade()
                    .into(holder.icon);

            holder.title.setText(appInfo.name);
            holder.desc.setText(appInfo.packageName);
        }

        @Override
        public int getItemCount() {
            return mPerAppList.size();
        }

        @Override
        public long getItemId(int position) {
            return mPerAppList.get(position).dbId;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView desc;

            public ViewHolder(View itemView) {
                super(itemView);

                icon = (ImageView) itemView.findViewById(R.id.image_app);
                title = (TextView) itemView.findViewById(R.id.text_name);
                desc = (TextView) itemView.findViewById(R.id.text_desc);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    }
}