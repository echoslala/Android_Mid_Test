## 期中作业
### 前情提要：

基础功能 ：

- 添加时间戳
- 搜索

扩展功能：

- 修改笔记字体大小、颜色
- 可挑选的笔记图片标签（学习、工作、个人、旅行）

效果演示：使用真机。

### 1. 基础要求

#### 1.1 添加时间戳

(1) 效果图：

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202111939594.png)

(2) 实现结果主要的相关思路/代码：

- 原本的数据库的创建时间的时间戳已经存在，我们只需要使用Cursor去调用，然后将时间戳格式化。

```java
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
```

- 在xml文件中需要为我们的笔记列表项添加一个文本框用于显示时间即可。

```xml
<TextView
            android:id="@+id/creation_time"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:attr/textAppearanceSmall"
            android:gravity="center_vertical"
            android:textColor="@android:color/darker_gray"
            android:layout_below="@android:id/text1" />

```



#### 1.2 添加搜索功能

(1) 效果图：

- 点击菜单栏搜索框的图标

- 进入一个新的页面
- 输入想查询的文字，使用的是模糊查询，呈现如下：
<img src="https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202123802476.png" alt="image-20241202123802476" style="zoom:33%;" />

(2) 实现结果主要的相关思路/代码：

基于项目模版原本的代码进行修改（菜单栏）：

- 创建搜索页面设计布局

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- 搜索框 -->
    <EditText
        android:id="@+id/search_box"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="输入搜索内容" />

    <!-- 搜索结果列表 -->
    <ListView
        android:id="@+id/search_results"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>
```

- 在菜单栏新增一个搜索图标，设置点击事件

```java
 // 设置搜索图标的点击事件
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
```

- 创建一个搜索的Activity (NoteSearch.java)

```java
public class NoteSearch extends Activity implements SearchView.OnQueryTextListener {
    private static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, //标题
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,//修改时间
    };
    private String[] mStrs = {};
    private SearchView mSearchView;
    private ListView lListView;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_search);
        mSearchView = (SearchView) findViewById(R.id.search_view);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setOnQueryTextListener(this);
        //mSearchView.setBackgroundColor(getResources().getColor());
        lListView = (ListView) findViewById(R.id.list);
        lListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mStrs));
        lListView.setTextFilterEnabled(true);
    }
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }
    @Override
    public boolean onQueryTextChange(String s) {
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " Like ? ";//查询条件
        String[] selectionArgs = { "%"+s+"%" };//查询条件参数，配合selection参数使用,%通配多个字符

        //查询数据库中的内容,当我们使用 SQLiteDatabase.query()方法时，就会得到Cursor对象， Cursor所指向的就是每一条数据。
        //managedQuery(Uri, String[], String, String[], String)等同于Context.getContentResolver().query()
        Cursor cursor = managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.用于ContentProvider查询的URI，从这个URI获取数据
                PROJECTION,                       // Return the note ID and title for each note. and modifcation date.用于标识uri中有哪些columns需要包含在返回的Cursor对象中
                selection,                        // 作为查询的过滤参数，也就是过滤出符合selection的数据，类似于SQL的Where语句之后的条件选择
                selectionArgs,                    // 查询条件参数，配合selection参数使用
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.查询结果的排序方式，按照某个columns来排序，例：String sortOrder = NotePad.Notes.COLUMN_NAME_TITLE
        );

        //一个简单的适配器，将游标中的数据映射到布局文件中的TextView控件或者ImageView控件中
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE ,  NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
        int[] viewIDs = { android.R.id.text1 , R.id.creation_time };
        adapter
                = new SimpleCursorAdapter(
                this,                           
                R.layout.noteslist_item,         
                cursor,                         
                dataColumns,
                viewIDs
        );
        lListView.setAdapter(adapter);
        return true;
    }
}

```



### 2. 扩展功能

#### 2.1 修改字体大小

(1) 效果实现流程：

- 点击菜单栏的那个【三个点】，会显示出设置菜单。

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/%E4%BF%AE%E6%94%B9%E5%A4%A7%E5%B0%8F%E5%92%8C%E9%A2%9C%E8%89%B2.jpg)

- 然后我们点击菜单中的【字体大小】，会出现如下的选择：

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202122343010.png)

- 我们选择【大】，字体变为如下：
<img src="https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202110516398.png" alt="image-20241202110516398" style="zoom:25%;" />

(2) 实现结果主要的相关思路/代码：

基于项目模版原本的代码进行修改（菜单栏）：

- 创建弹出菜单

```xml
<item
          android:id="@+id/font_size"
          android:title="字体大小">
          <!--子菜单-->
          <menu>
              <!--定义一组单选菜单项-->
              <group>
                  <!--定义多个菜单项-->
                  <item
                      android:id="@+id/font_10"
                      android:title="小"
                      />
  
                  <item
                      android:id="@+id/font_16"
                      android:title="中" />
                  <item
                      android:id="@+id/font_20"
                      android:title="大" />
              </group>
          </menu>
      </item>
```

- 设置菜单点击事件

```java
 private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.settings_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_font_size_small:
                        textContent.setTextSize(14); // 小字体
                        break;
                    case R.id.menu_font_size_medium:
                        textContent.setTextSize(16); // 中等字体
                        break;
                    case R.id.menu_font_size_large:
                        textContent.setTextSize(18); // 大字体
                        break;
                }
                return true;
            }
        });

        popupMenu.show();
    }
}
```



#### 2.2 修改字体颜色

(1) 效果图：
<img src="https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/%E5%8F%98%E7%BA%A2.jpg" alt="变红" style="zoom:25%;" />

（2）实现流程：

- 点击菜单栏的那个【三个点】，会显示出设置菜单。

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/%E4%BF%AE%E6%94%B9%E5%A4%A7%E5%B0%8F%E5%92%8C%E9%A2%9C%E8%89%B2.jpg)

- 然后我们点击菜单中的【字体颜色】，会出现如下的选择：

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/%E4%BF%AE%E6%94%B9%E9%A2%9C%E8%89%B2.jpg)

- 我们选择【红色】，字体变化如上图。

(3) 关键代码：

基于项目模版原本的代码进行修改（菜单栏）：

- 创建弹出菜单

```xml
<item
        android:title="字体颜色"
        android:id="@+id/font_color"
        >
        <menu>
            <!--定义一组普通菜单项-->
            <group>
                <!--定义两个菜单项-->
                <item
                    android:id="@+id/red_font"
                    android:title="红色" />
                <item
                    android:title="黑色"
                    android:id="@+id/black_font"/>
                <item
                    android:title="蓝色"
                    android:id="@+id/blue_font"/>
            </group>
        </menu>
    </item>
```

- 处理菜单选择事件

```java
private void showFontSizeMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.font_size_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.red_font:
                        textView.setTextColor(getResources().getColor(R.color.red));
                        break;
                    case R.id.black_font:
                        textView.setTextColor(getResources().getColor(R.color.black));
                        break;
                    case R.id.blue_font:
                        textView.setTextColor(getResources().getColor(R.color.blue));
                        break;
                }
                return true;
            }
        });

        popupMenu.show();
    }
```



#### 2.3 给笔记添加标签图片

(1) 效果图：

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202111939594.png)

(2) 实现流程：

- 点击菜单栏的这个图标，我们在代码中设置了点击事件。

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202112512387.png)

- 然后，会发现，屏幕中会弹出标签选择框，选择对应的标签即可。

  我们链接的图标图片会在笔记列表的笔记项的img部分来体现。

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202113647626.png)

（3）其中对应的标签：

- 学习（默认）：

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202113824157.png)

- 工作

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202113855940.png)

- 个人

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202113920511.png)

- 旅行

![image](https://github.com/echoslala/Android_Mid_Test/blob/master/github_pictures/image-20241202114001260.png)

（4）关键代码：

- 首先基于原先修改设计的笔记项布局（有img部分）

```xml
 <ImageView
            android:id="@+id/icon_menu"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_menu" />
```

- 创建弹出菜单资源

```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/menu_study"
        android:title="学习" />
    <item
        android:id="@+id/menu_personal"
        android:title="个人" />
    <item
        android:id="@+id/menu_work"
        android:title="工作" />
    <item
        android:id="@+id/menu_travel"
        android:title="旅行" />
</menu>
```

- 设置菜单图标的点击事件

```java
 // 设置菜单图标的点击事件
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.tag_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectedTag = item.getTitle().toString();
                updateAllNoteIcons();
                return true;
            }
        });

        popupMenu.show();
    }
```

- 链接笔记项的图标

```java
// 设置笔记列表项的图标
        noteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageView noteIcon = view.findViewById(R.id.note_icon);
                updateNoteIcon(noteIcon);
            }
        });

// 相关的设置
private void updateNoteIcon(ImageView noteIcon) {
        switch (selectedTag) {
            case "学习":
                noteIcon.setImageResource(R.drawable.study);
                break;
            case "个人":
                noteIcon.setImageResource(R.drawable.personal);
                break;
            case "工作":
                noteIcon.setImageResource(R.drawable.work);
                break;
            case "旅行":
                noteIcon.setImageResource(R.drawable.travel);
                break;
            default:
                noteIcon.setImageResource(R.drawable.study);
                break;
        }
    }
```

