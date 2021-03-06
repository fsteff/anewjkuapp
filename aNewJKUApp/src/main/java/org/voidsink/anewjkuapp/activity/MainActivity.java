/*
 *       ____.____  __.____ ___     _____
 *      |    |    |/ _|    |   \   /  _  \ ______ ______
 *      |    |      < |    |   /  /  /_\  \\____ \\____ \
 *  /\__|    |    |  \|    |  /  /    |    \  |_> >  |_> >
 *  \________|____|__ \______/   \____|__  /   __/|   __/
 *                   \/                  \/|__|   |__|
 *
 *  Copyright (c) 2014-2019 Paul "Marunjar" Pretsch
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.voidsink.anewjkuapp.activity;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voidsink.anewjkuapp.KusssAuthenticator;
import org.voidsink.anewjkuapp.KusssContentContract;
import org.voidsink.anewjkuapp.PoiContentContract;
import org.voidsink.anewjkuapp.PreferenceWrapper;
import org.voidsink.anewjkuapp.R;
import org.voidsink.anewjkuapp.analytics.Analytics;
import org.voidsink.anewjkuapp.base.BaseFragment;
import org.voidsink.anewjkuapp.base.StackedFragment;
import org.voidsink.anewjkuapp.base.ThemedActivity;
import org.voidsink.anewjkuapp.calendar.CalendarContractWrapper;
import org.voidsink.anewjkuapp.fragment.AssessmentFragment;
import org.voidsink.anewjkuapp.fragment.CalendarFragment;
import org.voidsink.anewjkuapp.fragment.CalendarFragment2;
import org.voidsink.anewjkuapp.fragment.CurriculaFragment;
import org.voidsink.anewjkuapp.fragment.ExamFragment;
import org.voidsink.anewjkuapp.fragment.LvaFragment;
import org.voidsink.anewjkuapp.fragment.MapFragment;
import org.voidsink.anewjkuapp.fragment.MensaFragment;
import org.voidsink.anewjkuapp.fragment.OehInfoFragment;
import org.voidsink.anewjkuapp.fragment.OehRightsFragment;
import org.voidsink.anewjkuapp.fragment.StatFragment;
import org.voidsink.anewjkuapp.utils.AppUtils;
import org.voidsink.anewjkuapp.utils.Consts;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends ThemedActivity {

    public static final String ARG_SHOW_FRAGMENT_ID = "show_fragment_id";
    public static final String ARG_EXACT_LOCATION = "exact_location";
    public static final String ARG_SAVE_LAST_FRAGMENT = "save_last_fragment";

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final int PERMISSIONS_REQUEST_ACCOUNT = 2;
    private static final String[] ACCOUNT_PERMISSIONS = {Manifest.permission.GET_ACCOUNTS};

    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    private DrawerLayout.DrawerListener mDrawerListener;
    private NavigationView mNavigationView;

    private static void StartMyCurricula(Context context) {
        //
        Intent i = new Intent(context, MainActivity.class)
                .putExtra(MainActivity.ARG_SHOW_FRAGMENT_ID, R.id.nav_curricula)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(i);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mNavigationView != null) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(Consts.ARG_FRAGMENT_TAG);
            if (f instanceof StackedFragment) {
                int id = ((StackedFragment) f).getId(this);
                if (id > 0) {
                    outState.putInt(ARG_SHOW_FRAGMENT_ID, id);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.debug(KusssContentContract.AUTHORITY);
        logger.debug(this.getResources().getString(R.string.config_kusss_provider));
        logger.debug(PoiContentContract.AUTHORITY);
        logger.debug(this.getResources().getString(R.string.config_poi_provider));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // set Statusbar color to transparent if a drawer exists
            // in this case status bar is colored by DrawerLayout.setStatusBarBackgroundColor(). which is default primaryDark
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.fullTransparent));
        }

        setContentView(R.layout.activity_main);

        // do things if new version was installed
        AppUtils.doOnNewVersion(this);

        // initialize graphic factory for mapsforge
        AndroidGraphicFactory.createInstance(this.getApplication());

        getSupportFragmentManager().addOnBackStackChangedListener(this::initActionBar);

        // set up drawer
        mDrawerLayout = findViewById(R.id.drawer_layout);

        mDrawerListener = new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                TextView mDrawerUser = mNavigationView.getHeaderView(0).findViewById(R.id.drawer_user);

                if (mDrawerUser != null) {
                    if ((android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)) {
                        mDrawerUser.setText(R.string.missing_app_permission);
                        mDrawerUser.setOnClickListener(v -> {
                            try {
                                MainActivity.this.startActivity(
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .addCategory(Intent.CATEGORY_DEFAULT)
                                                .setData(Uri.parse("package:org.voidsink.anewjkuapp")));
                            } catch (Exception e) {
                                Analytics.sendException(MainActivity.this, e, false);
                            }
                        });
                    } else {
                        Account account = AppUtils.getAccount(MainActivity.this);
                        if (account == null) {
                            mDrawerUser.setText(R.string.action_tap_to_login);
                            mDrawerUser.setOnClickListener(v -> startCreateAccount());
                        } else {
                            mDrawerUser.setText(account.name);
                            mDrawerUser.setOnClickListener(v -> MainActivity.StartMyCurricula(MainActivity.this));
                        }
                    }
                }

                super.onDrawerOpened(drawerView);
            }
        };

        mNavigationView = findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            setupDrawerContent(mNavigationView);
        }

        Intent intent = getIntent();

        Fragment f = attachFragment(intent, savedInstanceState, true);
        // attach calendar fragment as default
        if (f == null) {
            f = attachFragmentById(R.id.nav_cal, true);
        }
        handleIntent(f, intent);

        startCreateAccount();

        logger.debug("onCreate finished");
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_ACCOUNT)
    private void startCreateAccount() {
        if ((android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) || EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            if (AppUtils.getAccount(this) == null) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    this.startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT)
                            .putExtra(Settings.EXTRA_ACCOUNT_TYPES,
                                    new String[]{KusssAuthenticator.ACCOUNT_TYPE}));
                } else {
                    this.startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT)
                            .putExtra(Settings.EXTRA_AUTHORITIES,
                                    new String[]{CalendarContractWrapper.AUTHORITY()}));
                }
            }
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.alert_permission_get_accounts),
                    PERMISSIONS_REQUEST_ACCOUNT,
                    ACCOUNT_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private Fragment attachFragmentById(int id, boolean saveLastFragment) {
        if (mNavigationView != null) {
            MenuItem mMenuItem = mNavigationView.getMenu().findItem(id);

            return attachFragment(mMenuItem, saveLastFragment);
        }
        return null;
    }

    private void setupDrawerContent(NavigationView navigationView) {
        // set a custom shadow that overlays the main content when the drawer
        // opens
        // mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
        // GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp);
        actionBar.setHomeButtonEnabled(true);

        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.nav_settings: {
                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                            break;
                        }
                        case R.id.nav_about: {
                            startActivity(new Intent(MainActivity.this, AboutActivity.class));
                            break;
                        }
                        default:
                            attachFragment(menuItem, true);
                            break;
                    }

                    mDrawerLayout.closeDrawers();
                    return menuItem.isCheckable();
                });
    }

    private Fragment attachFragment(Intent intent, Bundle savedInstanceState,
                                    boolean attachStored) {
        if (intent != null && intent.hasExtra(ARG_SHOW_FRAGMENT_ID)) {
            // show fragment from intent
            return attachFragmentById(
                    intent.getIntExtra(ARG_SHOW_FRAGMENT_ID, 0),
                    intent.getBooleanExtra(ARG_SAVE_LAST_FRAGMENT, true));
        } else if (savedInstanceState != null) {
            // restore saved fragment
            return attachFragmentById(savedInstanceState
                    .getInt(ARG_SHOW_FRAGMENT_ID), true);
        } else if (attachStored) {
            return attachFragmentById(PreferenceWrapper
                    .getLastFragment(this), true);
        } else {
            return getSupportFragmentManager().findFragmentByTag(
                    Consts.ARG_FRAGMENT_TAG);
        }
    }

    private void handleIntent(Fragment f, Intent intent) {
        if (f == null) {
            f = getSupportFragmentManager()
                    .findFragmentByTag(Consts.ARG_FRAGMENT_TAG);
        }

        if (f instanceof BaseFragment) {
            ((BaseFragment) f).handleIntent(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Fragment f = attachFragment(intent, null, false);
        handleIntent(f, intent);
    }

    private Fragment attachFragment(MenuItem menuItem, boolean saveLastFragment) {

        if (menuItem == null) {
            return null;
        }

        Class<? extends Fragment> startFragment = null;

        switch (menuItem.getItemId()) {
            case R.id.nav_cal:
                if (PreferenceWrapper.getUseCalendarView(this)) {
                    startFragment = CalendarFragment2.class;
                } else {
                    startFragment = CalendarFragment.class;
                }
                break;
            case R.id.nav_exams:
                startFragment = ExamFragment.class;
                break;
            case R.id.nav_grades:
                startFragment = AssessmentFragment.class;
                break;
            case R.id.nav_courses:
                startFragment = LvaFragment.class;
                break;
            case R.id.nav_stats:
                startFragment = StatFragment.class;
                break;
            case R.id.nav_mensa:
                startFragment = MensaFragment.class;
                break;
            case R.id.nav_map:
                startFragment = MapFragment.class;
                break;
            case R.id.nav_oeh_info:
                startFragment = OehInfoFragment.class;
                break;
            case R.id.nav_oeh_rigths:
                startFragment = OehRightsFragment.class;
                break;
            case R.id.nav_curricula:
                startFragment = CurriculaFragment.class;
                break;
            default:
                break;
        }

        if (startFragment != null) {
            try {
                final Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(Consts.ARG_FRAGMENT_TAG);

                if (oldFragment != null) {
                    if (startFragment.getCanonicalName().equals(oldFragment.getClass().getCanonicalName())) {
                        return oldFragment;
                    }
                }

                Fragment f = startFragment.getConstructor().newInstance();

                Bundle b = new Bundle();
                b.putCharSequence(Consts.ARG_FRAGMENT_TITLE, menuItem.getTitle());
                b.putInt(Consts.ARG_FRAGMENT_ID, menuItem.getItemId());
                f.setArguments(b);

                final boolean addToBackstack = (oldFragment != null) && !oldFragment.getClass().equals(f.getClass());

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, f, Consts.ARG_FRAGMENT_TAG);
                if (addToBackstack) {
                    ft.addToBackStack(f.getClass().getCanonicalName());
                }
                ft.commit();

                if (saveLastFragment) {
                    PreferenceWrapper.setLastFragment(this, menuItem.getItemId());
                }

                initActionBar();

                return f;
            } catch (Exception e) {
                logger.warn("fragment instantiation failed", e);
                Analytics.sendException(this, e, false);
                if (saveLastFragment) {
                    PreferenceWrapper.setLastFragment(this, PreferenceWrapper.PREF_LAST_FRAGMENT_DEFAULT);
                }
                return null;
            }
        }
        return null;
    }


    @Override
    protected void onInitActionBar(ActionBar actionBar) {
        super.onInitActionBar(actionBar);

        actionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        initActionBar();

        menu.clear();
        getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDrawerLayout.addDrawerListener(mDrawerListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        mDrawerLayout.removeDrawerListener(mDrawerListener);

        Analytics.clearScreen(this);
    }
}
