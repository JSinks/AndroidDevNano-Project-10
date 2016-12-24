package cool.superfcking.apps.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import cool.superfcking.apps.inventory.data.InventoryContract.InventoryEntry;

/**
 * Created by jsinclair on 23/12/16.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    private Context mContext;

    private final static String LOG_TAG = InventoryCursorAdapter.class.getSimpleName();

    public InventoryCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.inventory_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameText = (TextView) view.findViewById(R.id.name);
        TextView quantityText = (TextView) view.findViewById(R.id.quantity);
        TextView priceText = (TextView) view.findViewById(R.id.price);

        String name = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME));
        String price = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY));
        final int id = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));

        nameText.setText(name);
        quantityText.setText(String.format(
                context.getString(R.string.quantity_text),
                Integer.toString(quantity)
        ));
        priceText.setText(String.format(context.getString(R.string.price_text), price));

        Button yourButton = (Button) view.findViewById(R.id.btn_inventory_sell);
        yourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(quantity > 0){
                    Uri updateUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, quantity - 1);

                    int rowsUpdated = mContext.getContentResolver().update(updateUri, values, null, null);
                    Log.v(LOG_TAG, "Rows updated: " + rowsUpdated);
                } else {
                    Log.v(LOG_TAG, "No quantity change, already at 0");
                }

            }
        });
    }


}
