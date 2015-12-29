package com.pluscubed.anticipate;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.transitions.FabDialogMorphSetup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PerAppListActivity extends AppCompatActivity {

    public static final String DB = "Anticipate";
    public static final String TABLE_BLACKLISTED_APPS = "BlacklistedApps";
    public static final String TABLE_WHITELISTED_APPS = "WhitelistedApps";

    List<AppPackage> mPerAppList;
    AppAdapter mAdapter;

    boolean mBlacklistMode;
    private FloatingActionButton mFab;
    private AppBarLayout mAppBar;


    @Override
    protected void onResume() {
        super.onResume();

        mAdapter.notifyDataSetChanged();
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


        Single.create(new Single.OnSubscribe<List<AppPackage>>() {
            @Override
            public void call(SingleSubscriber<? super List<AppPackage>> singleSubscriber) {
                String table = mBlacklistMode ? TABLE_BLACKLISTED_APPS : TABLE_WHITELISTED_APPS;

                AppPackage[] all = Inquiry.get().selectFrom(table, AppPackage.class)
                        .all();
                if (all != null) {
                    singleSubscriber.onSuccess(new ArrayList<>(Arrays.asList(all)));
                } else {
                    singleSubscriber.onSuccess(new ArrayList<AppPackage>());
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppPackage>>() {
                    @Override
                    public void onSuccess(List<AppPackage> value) {
                        mPerAppList = value;

                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });

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
                startActivity(intent, options);
            }
        });

        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
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
            AppPackage app = mPerAppList.get(position);

            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(app.package_name, 0);

                holder.icon.setImageDrawable(info.loadIcon(getPackageManager()));
                holder.title.setText(info.loadLabel(getPackageManager()));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            holder.desc.setText(app.package_name);

        }

        @Override
        public int getItemCount() {
            return mPerAppList.size();
        }

        @Override
        public long getItemId(int position) {
            return mPerAppList.get(position).id;
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
