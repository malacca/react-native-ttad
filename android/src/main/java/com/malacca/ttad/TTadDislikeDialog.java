package com.malacca.ttad;

import java.util.List;
import java.util.ArrayList;

import android.os.Bundle;
import android.view.Window;
import android.graphics.Color;
import android.widget.TextView;
import android.content.Context;
import androidx.annotation.NonNull;

import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.bytedance.sdk.openadsdk.FilterWord;
import com.bytedance.sdk.openadsdk.TTDislikeDialogAbstract;
import com.bytedance.sdk.openadsdk.dislike.TTDislikeListView;

/**
 * 自定义 dislike dialog 来源于 sdk demo 包
 * 自带的那个弹出 ui 细节有点差, 所以默认的 dislike 弹窗改用这个
 */
class TTadDislikeDialog extends TTDislikeDialogAbstract {
    private final List<FilterWord> mList;
    private OnDislikeItemClick mOnDislikeItemClick;

    TTadDislikeDialog(@NonNull Context context, List<FilterWord> list) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mList = initData(list);
    }

    //目前网盟的负反馈存在二级选项，需要特殊处理
    private List<FilterWord> initData(List<FilterWord> list) {
        List<FilterWord> data = new ArrayList<>();
        if (list != null) {
            for (FilterWord filterWord : list) {
                if (filterWord.hasSecondOptions()) {
                    data.addAll(initData(filterWord.getOptions()));
                } else {
                    data.add(filterWord);
                }
            }
        }
        return data;
    }

    void setOnDislikeItemClick(OnDislikeItemClick onDislikeItemClick) {
        mOnDislikeItemClick = onDislikeItemClick;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TTDislikeListView listView = findViewById(R.id.lv_dislike_custom);
        listView.setAdapter(new MyDislikeAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TTadDislikeDialog.this.dismiss();
                if (mOnDislikeItemClick != null) {
                    FilterWord word = null;
                    try {
                        word = (FilterWord) parent.getAdapter().getItem(position);
                    } catch (Throwable ignore) {
                    }
                    mOnDislikeItemClick.onItemClick(word);
                }
            }
        });
    }

    @Override
    public int getLayoutId() {
        return R.layout.dlg_dislike_custom;
    }

    @Override
    public int[] getTTDislikeListViewIds() {
        return new int[]{R.id.lv_dislike_custom};
    }

    @Override
    public ViewGroup.LayoutParams getLayoutParams() {
        return null;
    }

    class MyDislikeAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList == null ? null : mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FilterWord word = (FilterWord) getItem(position);
            TextView textView = new TextView(getContext());
            textView.setPadding(0, 40, 0, 40);
            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER);
            textView.setText(word.getName());
            return textView;
        }
    }

    public interface OnDislikeItemClick {
        void onItemClick(FilterWord filterWord);
    }
}
