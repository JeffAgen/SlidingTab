package com.xhz.slidingtab;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import org.xhz.slidingtab.PagerTitleBar;

public class MainActivity extends AppCompatActivity {
    private static final int TAB_COUNT = 3;
    private static final int HOME_TAB_POSITION = 0;
    private static final int CHAT_TAB_POSITION = 1;
    private static final int ABOUT_TAB_POSITION = 2;
    private static final int[] mTitleResIds = new int[]{
            R.string.home_tab_title,
            R.string.chat_tab_title,
            R.string.about_tab_title};
    private static final int[] mIconResIds = new int[]{
            R.drawable.sel_home,
            R.drawable.sel_chat,
            R.drawable.sel_about};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewPager();
    }

    private void initViewPager() {
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        PagerTitleBar pagerTitleBar = (PagerTitleBar) findViewById(R.id.tab_bar);
        if (viewPager == null || pagerTitleBar == null) return;

        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(TAB_COUNT - 1);
        viewPager.setCurrentItem(HOME_TAB_POSITION);
        pagerTitleBar.setViewPager(viewPager);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter implements PagerTitleBar.ICustomTab {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == HOME_TAB_POSITION) {
                return HomeFragment.newInstance();
            } else if (position == CHAT_TAB_POSITION) {
                return ChatFragment.newInstance();
            } else if (position == ABOUT_TAB_POSITION) {
                return AboutFragment.newInstance();
            }
            return HomeFragment.newInstance();
        }

        @Override
        public int getCount() {
            return TAB_COUNT;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getString(mTitleResIds[position]);
        }

        @Override
        public Drawable getIcon(int position) {
            return ContextCompat.getDrawable(MainActivity.this, mIconResIds[position]);
        }
    }
}
