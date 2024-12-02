/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * 这个Activity允许用户编辑笔记的标题。它显示一个包含EditText的浮动窗口。
 *
 * 注意：请注意，在这个Activity中进行的提供者操作是在UI线程上进行的。
 * 这不是一个好的做法。这样做只是为了使代码更易读。在实际应用中，应该使用
 * android.content.AsyncQueryHandler 或 android.os.AsyncTask 对象在单独的线程上异步执行操作。
 */
public class TitleEditor extends Activity {

    /**
     * 这是一个特殊的意图动作，表示"编辑笔记的标题"。
     */
    public static final String EDIT_TITLE_ACTION = "com.android.notepad.action.EDIT_TITLE";

    // 创建一个投影，返回笔记ID和笔记内容。
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
    };

    // 在提供者返回的Cursor中，标题列的位置。
    private static final int COLUMN_INDEX_TITLE = 1;

    // 一个Cursor对象，将包含查询提供者以获取笔记的结果。
    private Cursor mCursor;

    // 一个EditText对象，用于保留编辑后的标题。
    private EditText mText;

    // 一个URI对象，表示正在编辑标题的笔记。
    private Uri mUri;

    /**
     * 当Activity首次启动时，Android会调用此方法。从传入的Intent中，它确定所需的编辑类型，然后执行它。
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置此Activity对象UI的视图。
        setContentView(R.layout.title_editor);

        // 获取激活此Activity的Intent，并从中获取需要编辑标题的笔记的URI。
        mUri = getIntent().getData();

        /*
         * 使用触发Intent传递的URI，获取笔记。
         *
         * 注意：这正在UI线程上进行。它将阻塞线程，直到查询完成。在示例应用中，针对基于本地数据库的简单提供者，
         * 阻塞将是瞬间的，但在实际应用中，您应该使用
         * android.content.AsyncQueryHandler 或 android.os.AsyncTask。
         */

        mCursor = managedQuery(
                mUri,        // 要检索的笔记的URI。
                PROJECTION,  // 要检索的列。
                null,        // 没有使用选择标准，所以不需要where列。
                null,        // 没有使用where列，所以不需要where值。
                null         // 不需要排序顺序。
        );

        // 获取EditText框的视图ID
        mText = (EditText) this.findViewById(R.id.title);
    }

    /**
     * 当Activity即将来到前台时调用此方法。这发生在Activity来到任务栈顶部时，或者它第一次启动时。
     *
     * 显示选定的笔记的当前标题。
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 验证在onCreate()中进行的查询实际上是否成功。如果成功，则Cursor对象不为null。
        // 如果它是空的，则mCursor.getCount() == 0。
        if (mCursor != null) {

            // Cursor刚刚被检索，所以它的索引设置为在第一个检索到的记录之前。
            // 这将移动到第一个记录。
            mCursor.moveToFirst();

            // 在EditText对象中显示当前标题文本。
            mText.setText(mCursor.getString(COLUMN_INDEX_TITLE));
        }
    }

    /**
     * 当Activity失去焦点时调用此方法。
     *
     * 对于编辑信息的Activity对象，onPause()可能是保存更改的地方。Android应用程序模型基于这样一个概念：
     * "保存"和"退出"不是必需的操作。当用户从Activity导航离开时，他们不应该返回来完成他们的工作。
     * 离开的动作应该保存所有内容，并使Activity处于一个状态，Android可以在必要时销毁它。
     *
     * 使用文本框中的当前文本更新笔记。
     */
    @Override
    protected void onPause() {
        super.onPause();

        // 验证在onCreate()中进行的查询实际上是否成功。如果成功，则
// Cursor对象不为null。如果它是空的，则mCursor.getCount() == 0。

        if (mCursor != null) {

            // 创建一个值映射，用于更新提供者。
            ContentValues values = new ContentValues();

            // 在值映射中，将标题设置为编辑框中的当前内容。
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, mText.getText().toString());

            /*
             * 使用笔记的新标题更新提供者。
             *
             * 注意：这正在UI线程上进行。它将阻塞线程，直到更新完成。在示例应用中，针对基于本地数据库的简单提供者，
             * 阻塞将是瞬间的，但在实际应用中，您应该使用
             * android.content.AsyncQueryHandler 或 android.os.AsyncTask。
             */
            getContentResolver().update(
                    mUri,    // 要更新的笔记的URI。
                    values,  // 包含要更新的列和值的值映射。
                    null,    // 没有使用选择标准，所以不需要where列。
                    null     // 没有使用where列，所以不需要where值。
            );

        }
    }

    public void onClickOk(View v) {
        finish();
    }
}
