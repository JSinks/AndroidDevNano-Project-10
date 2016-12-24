package cool.superfcking.apps.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.design.widget.FloatingActionButton;

import cool.superfcking.apps.inventory.data.InventoryContract.InventoryEntry;

public class InventoryView extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static ListView mListView;
    private static InventoryCursorAdapter mCursorAdapter;
    private final static int LOADER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_view);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryView.this, DetailView.class);
                startActivity(intent);
            }
        });

        mListView = (ListView) findViewById(R.id.list_view);
        View emptyView = findViewById(R.id.empty_list);
        mListView.setEmptyView(emptyView);

        mListView.setOnItemClickListener(new ListView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(InventoryView.this, DetailView.class);

                Uri uri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, l);
                intent.setData(uri);

                startActivity(intent);
            }
        });

        // Attach cursor adapter to the ListView
        mCursorAdapter = new InventoryCursorAdapter(this, null, true);
        mListView.setAdapter(mCursorAdapter);

        getLoaderManager().initLoader(LOADER_ID, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri baseUri = InventoryEntry.CONTENT_URI;
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_PRODUCT_PRICE
        };

        return new CursorLoader(this, baseUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

//    TODO: figure out how to store and reference images - store in db?
//    TODO: add "track sale" action which decrements the qty value
//    TODO: hide track sale button when qty is 0

}
