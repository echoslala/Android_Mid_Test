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

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 显示笔记列表。如果传入的Intent中提供了{@link Uri}，则显示该Uri中的笔记，
 * 否则默认显示{@link NotePadProvider}的内容。
 * <p>
 * 注意：请注意，此Activity中的提供者操作发生在UI线程上。
 * 这不是一个好的做法。这里只是为了使代码更易读。实际应用应该使用
 * {@link android.content.AsyncQueryHandler}或
 * {@link android.os.AsyncTask}对象在单独的线程上异步执行操作。
 */
public class NotesList extends ListActivity {

    // 用于日志和调试
    private static final String TAG = "NotesList";

    /**
     * 光标适配器所需的列
     */
    private static final String[] PROJECTION = new String[]{NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_CREATE_DATE// 1
    };

    /**
     * 标题列的索引
     */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * onCreate在Android从头开始启动此Activity时被调用。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 用户无需按住键即可使用菜单快捷键。
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* 如果启动此Activity的Intent中没有提供数据，则此Activity是在匹配了MAIN操作时启动的。
         * 我们应该使用默认的提供者URI。
         */
        // 获取启动此Activity的Intent。
        Intent intent = getIntent();

        // 如果Intent中没有与数据相关联，则将数据设置为默认URI，这将访问笔记列表。
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * 设置ListView的上下文菜单激活回调。监听器设置为这个Activity。
         * 这样做的作用是启用ListView中项目的上下文菜单，并且由NotesList中的方法处理上下文菜单。
         */
        getListView().setOnCreateContextMenuListener(this);

        /* 执行托管查询。Activity处理关闭和在需要时重新查询光标。
         *
         * 请参阅关于在UI线程上执行提供者操作的 introductory note。
         */
        Cursor cursor = managedQuery(getIntent().getData(),            // 使用提供者的默认内容URI。
                PROJECTION,                       // 返回每个笔记的笔记ID和标题。
                null,                             // 没有where子句，返回所有记录。
                null,                             // 没有where子句，因此没有where列值。
                NotePad.Notes.DEFAULT_SORT_ORDER  // 使用默认的排序顺序。
        );

        /*
         * 以下两个数组创建了一个“映射”，将光标中的列与ListView中项目的视图ID关联起来。
         * dataColumns数组中的每个元素代表一个列名；viewID数组中的每个元素代表一个视图的ID。
         * SimpleCursorAdapter按升序映射它们，以确定每个列值在ListView中出现的位置。
         */

        // 要在视图中显示的光标列的名称，初始化为标题列
        String[] dataColumns = {NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_CREATE_DATE};
        //String[] dataColumns = {NotePad.Notes.COLUMN_NAME_TITLE};

         // 将显示光标列的视图ID，初始化为noteslist_item.xml中的TextView
        int[] viewIDs = {android.R.id.text1, R.id.creation_time};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs

        ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);
                // 检查并设置默认值
                TextView creationTimeTextView = (TextView) view.findViewById(R.id.creation_time);
                //long creationTime = cursor.getLong(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CREATE_DATE));
                long creationTime = cursor.getLong(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CREATE_DATE));

                if (creationTime == 0) {
                    creationTimeTextView.setText("未知日期"); // 设置为空字符串
                } else {
                    // 格式化时间
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = new Date(creationTime);
                    creationTimeTextView.setText(dateFormat.format(date));
                }
            }
        };

        // 将ListView的适配器设置为刚创建的光标适配器。
        setListAdapter(adapter);

    }


    /**
     * 搜索框
     */



    /**
     * 当用户第一次点击设备的菜单按钮时调用此方法。
     * Android传递一个填充了项的Menu对象。
     * <p>
     * 设置一个菜单，提供插入选项以及此Activity的替代操作列表。
     * 其他希望处理笔记的应用程序可以通过提供包含类别ALTERNATIVE和
     * mimeTYpe NotePad.Notes.CONTENT_TYPE的intent filter在Android中“注册”自己。
     * 如果它们这样做，onCreateOptionsMenu()中的代码将添加包含intent filter的Activity到其选项列表中。
     * 实际上，菜单将向用户提供其他可以处理笔记的应用程序。
     *
     * @param menu 要添加菜单项的Menu对象。
     * @return 总是返回True。应该显示菜单。
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 从XML资源中填充菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 生成可以在整个列表上执行的其他任何操作。
        // 在正常安装中，这里没有找到任何其他操作，但这允许其他应用程序用自己的操作扩展我们的菜单。
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // 如果剪贴板包含一个项目，则启用菜单上的粘贴选项。
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // 如果剪贴板包含一个项目，则启用菜单上的粘贴选项。
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // 如果剪贴板为空，则禁用菜单上的粘贴选项。
            mPasteItem.setEnabled(false);
        }

        // 获取当前显示的笔记数量。
        final boolean haveItems = getListAdapter().getCount() > 0;

        // 如果列表中有任何笔记（这意味着其中之一被选中），则需要生成可以对当前选择执行的操作。
        // 这将是我们的特定操作和可以找到的任何扩展的组合。
        if (haveItems) {

            // 这是选中的项目。
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // 创建一个包含一个元素的Intent数组。这将用于基于选中的菜单项发送Intent。
            Intent[] specifics = new Intent[1];

            // 将数组中的Intent设置为对选中笔记URI的EDIT操作。
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // 创建一个包含一个元素的菜单项数组。这将包含EDIT选项。
            MenuItem[] items = new MenuItem[1];

            // 使用选中笔记的URI创建一个没有特定操作的Intent。
            Intent intent = new Intent(null, uri);

            /* 将类别ALTERNATIVE添加到Intent中，以笔记ID URI作为其数据。
             * 这将Intent准备为一个地方，在菜单中分组替代选项。
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * 将替代项添加到菜单中
             */
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE,  // 将Intent作为选项添加到替代组中。
                    Menu.NONE,                  // 不需要唯一的项目ID。
                    Menu.NONE,                  // 替代项不需要按顺序排列。
                    null,                       // 调用者的名称不排除在组之外。
                    specifics,                  // 这些特定选项必须首先出现。
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // 如果编辑菜单项存在，则为它添加快捷方式。
            if (items[0] != null) {

                // 将编辑菜单项的快捷方式设置为数字“1”，字母“e”
                items[0].setShortcut('1', 'e');
            } else {
                // 如果列表为空，则从菜单中移除任何现有的替代操作
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }
        }
// 显示菜单
        return true;
    }

    /**
     * 当用户从菜单中选择一个选项但列表中没有选中任何项目时，调用此方法。
     * 如果选项是INSERT，则发送一个新的Intent，其操作为ACTION_INSERT。
     * 将传入Intent中的数据放入新Intent中。实际上，这将触发NotePad应用程序中的NoteEditor活动。
     * <p>
     * 如果选项不是INSERT，那么它很可能是来自另一个应用程序的替代选项。调用父方法来处理该项目。
     *
     * @param item 用户选择的菜单项
     * @return 如果选择了INSERT菜单项，则返回True；否则，返回调用父方法的结果。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                /*
                 * 使用Intent启动一个新的Activity。Intent filter for the Activity
                 * 必须有ACTION_INSERT操作。没有设置类别，因此假设为DEFAULT。
                 * 实际上，这启动了NotePad中的NoteEditor Activity。
                 */
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:
                /*
                 * 使用Intent启动一个新的Activity。Intent filter for the Activity
                 * 必须有ACTION_PASTE操作。没有设置类别，因此假设为DEFAULT。
                 * 实际上，这启动了NotePad中的NoteEditor Activity。
                 */
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 当用户在列表中长按一个笔记时，会调用此方法。NotesList将自己注册为其ListView上下文菜单的处理程序（这是在onCreate()中完成的）。
     * <p>
     * 可用的选项只有复制（COPY）和删除（DELETE）。
     * <p>
     * 上下文点击相当于长按。
     *
     * @param menu     应添加项目的上下文菜单对象。
     * @param view     正在为其构建上下文菜单的视图。
     * @param menuInfo 与视图关联的数据。
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // 菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        // 尝试获取在ListView中被长按的项目的位置。
        try {
            // 将传入的数据对象转换为AdapterView对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // 如果菜单对象无法转换，记录一个错误。
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * 获取与选定的位置的项目关联的数据。getItem()返回列表视图的备份适配器与项目关联的任何内容。
         * 在NotesList中，适配器将笔记的所有数据与其列表项关联。因此，getItem()返回该数据作为游标。
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // 如果游标为空，那么由于某种原因适配器无法从提供程序获取数据，因此返回null给调用者。
        if (cursor == null) {
            // 由于某种原因请求的项目不可用，不执行任何操作
            return;
        }

        // 从XML资源填充菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // 将菜单标题设置为选定笔记的标题。
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // 添加到菜单项，用于其他活动也可以对其进行操作。
        // 这对系统进行查询，查找任何实现我们数据的ALTERNATIVE_ACTION的活动，
        // 为每个找到的活动添加一个菜单项。
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(), Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * 当用户从上下文菜单中选择一个项目时，会调用此方法（请参见onCreateContextMenu()）。实际上处理的菜单项只有删除（DELETE）和复制（COPY）。其他任何内容都是替代选项，应该进行默认处理。
     *
     * @param item 选中的菜单项
     * @return 如果菜单项是删除（DELETE），则不需要默认处理，返回true；否则返回false，触发对项目的默认处理。
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 菜单项的数据。
        AdapterView.AdapterContextMenuInfo info;

        /*
         * 从菜单项获取额外信息。当用户在笔记列表中长按一个笔记时，会出现上下文菜单。菜单项自动获取与长按的笔记相关联的数据。
         * 数据来自支持列表的提供程序。
         *
         * 笔记的数据通过ContextMenuInfo对象传递给上下文菜单创建例程。
         *
         * 当点击上下文菜单中的一个项目时，相同的数据连同笔记ID一起通过item参数传递给onContextItemSelected()。
         */
        try {
            // 将item中的数据对象转换为AdapterView对象的类型。
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // 如果对象无法转换，记录一个错误
            Log.e(TAG, "bad menuInfo", e);

            // 触发菜单项的默认处理。
            return false;
        }
        // 将选中的笔记的ID附加到传入Intent的URI。
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * 获取菜单项的ID并与已知操作进行比较。
         */
        switch (item.getItemId()) {
            case R.id.context_open:
                // 启动活动以查看/编辑当前选中的项目
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
//BEGIN_INCLUDE(copy)
            case R.id.context_copy:
                // 获取剪贴板服务的句柄。
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                // 将笔记URI复制到剪贴板。实际上，这复制了笔记本身
                clipboard.setPrimaryClip(ClipData.newUri(   // 新的剪贴板项目，包含URI
                        getContentResolver(),               // 用于检索URI信息的解析器
                        "Note",                             // 剪贴板的标签
                        noteUri)                            // URI
                );

                // 返回给调用者并跳过进一步处理。
                return true;
//END_INCLUDE(copy)
            case R.id.context_delete:

                // 通过传递笔记ID格式的URI从提供程序中删除笔记。
                // 请参阅关于在UI线程上执行提供程序操作的介绍性注释。
                getContentResolver().delete(noteUri,  // 提供程序的URI
                        null,     // 由于只传递了一个笔记ID，所以不需要where子句。
                        null      // 由于没有使用where子句，所以不需要where参数。
                );

                // 返回给调用者并跳过进一步处理。
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * 当用户点击显示列表中的一个笔记时，会调用此方法。
     * <p>
     * 此方法处理传入的操作，无论是PICK（从提供程序获取数据）还是GET_CONTENT（获取或创建数据）。如果传入的操作是EDIT，此方法发送一个新的Intent以启动NoteEditor。
     *
     * @param l        包含点击项的ListView
     * @param v        单个项的View
     * @param position v在显示列表中的位置
     * @param id       点击项的行ID
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // 从传入的URI和行ID构建一个新的URI
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // 从传入的Intent获取操作
        String action = getIntent().getAction();

        // 处理请求笔记数据
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // 设置结果以返回给调用此Activity的组件。结果包含新的URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // 发送一个Intent以启动可以处理ACTION_EDIT的活动。Intent的数据是笔记ID URI。效果是调用NoteEdit。
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

}
