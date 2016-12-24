package cool.superfcking.apps.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

import cool.superfcking.apps.inventory.data.InventoryContract.InventoryEntry;

public class DetailView extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailView.class.getSimpleName();
    private static final int SELECT_PICTURE = 100;

    private Uri mUri;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the products's available quantity*/
    private EditText mQuantityEditText;

    /** EditText field to enter the products's unit price */
    private EditText mPriceEditText;

    private ImageView mProductImage;
    private Uri mImageUri;

    private int mStartingQuantity;

    private Button mSellButton;
    private Button mReceiveButton;

    // TODO: add buttons for increasing and decreasing quantity
    // TODO: add picture handling (insert new picture, and retrieve existing picture)

    private final static int PRODUCT_LOADER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mReceiveButton = (Button) findViewById(R.id.btn_detail_receive);
        mSellButton = (Button) findViewById(R.id.btn_detail_sell);
        mProductImage = (ImageView) findViewById(R.id.product_image);

        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values = new ContentValues();
                values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, mStartingQuantity + 1);
                getContentResolver().update(mUri, values, null, null);
            }
        });

        mSellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values = new ContentValues();
                values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, mStartingQuantity - 1);
                getContentResolver().update(mUri, values, null, null);
            }
        });

        mProductImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageChooser();
            }
        });

        // Get Uri from Intent
        Uri productUri = getIntent().getData();
        if (productUri != null) {
            setTitle(getString(R.string.title_edit_product));
            mReceiveButton.setVisibility(View.VISIBLE);
            mSellButton.setVisibility(View.VISIBLE);

            mUri = productUri;
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        } else {
            setTitle(getString(R.string.title_add_product));
            mReceiveButton.setVisibility(View.INVISIBLE);
            mSellButton.setVisibility(View.INVISIBLE);
            mProductImage.setImageResource(R.drawable.ic_add_a_photo_black_24dp);

            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mUri == null) {
            MenuItem orderItem = menu.findItem(R.id.action_order);
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            orderItem.setVisible(false);
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                saveProduct();
                return true;

            // Respond to a click on the "Order" menu option
            case R.id.action_order:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");

                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
                intent.putExtra(
                        Intent.EXTRA_TEXT,
                        String.format(
                                getString(R.string.email_body),
                                mNameEditText.getText()
                        )
                );

                Intent mailer = Intent.createChooser(intent, null);
                startActivity(mailer);
                return true;


            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                DialogInterface.OnClickListener deleteButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Delete" button, navigate to parent activity.
                                deleteProduct();
                                finish();
                            }
                        };

                showConfirmDeleteDialog(deleteButtonClickListener);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void saveProduct(){
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        // If no fields have been filled in, then error out
        if(nameString.isEmpty() &&
                quantityString.isEmpty() &&
                priceString.isEmpty() &&
                (mImageUri == null || mUri == null)){
            finish();
        }

        // If any one field is missing, but not all - then show an error
        if(nameString.isEmpty() ||
                quantityString.isEmpty() ||
                priceString.isEmpty() ||
                (mImageUri == null && mUri == null)){
            Toast.makeText(this, getString(R.string.help_fields_missing), Toast.LENGTH_SHORT).show();
        } else {
            //Safe to extract the byte string
            byte[] imageData = null;
            try {
                InputStream iStream = getContentResolver().openInputStream(mImageUri);
                imageData = ImageUtils.getBytes(iStream);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "File not Found Exception occured when trying to get byte stream");
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO Exception occured when trying to get byte stream");
            }

            ContentValues values = new ContentValues();
            values.put(InventoryEntry.COLUMN_PRODUCT_NAME, nameString);
            values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
            values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, priceString);
            values.put(InventoryEntry.COLUMN_PRODUCT_IMAGE, imageData);

            String toastText;
            if (mUri == null) {
                Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
                if (newUri != null){
                    toastText = getString(R.string.toast_product_added);
                } else {
                    toastText = getString(R.string.toast_product_add_error);
                }
            } else {
                int rowsUpdated = getContentResolver().update(mUri, values, null, null);
                if (rowsUpdated > 0){
                    toastText = getString(R.string.toast_product_updated);
                } else {
                    toastText = getString(R.string.toast_product_update_error);
                }
            }

            Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void deleteProduct(){
        if (mUri != null) {
            int rowsDeleted = getContentResolver().delete(mUri, null, null);

            if (rowsDeleted > 0) {
                Toast.makeText(this, "Product(s) Deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error Deleting Product(s)", Toast.LENGTH_SHORT).show();
            }

            finish();
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_IMAGE
        };

        return new CursorLoader(this, mUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME));
            Integer quantity = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY));
            Integer price = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE));
            byte[] imageBlob = cursor.getBlob(cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_IMAGE));


            mStartingQuantity = quantity;
            mNameEditText.setText(name);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));
            mProductImage.setImageBitmap(ImageUtils.getImage(imageBlob));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText(null);
        mQuantityEditText.setText(null);
        mPriceEditText.setText(null);
    }

    private void showConfirmDeleteDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.confirm_deletion);
        builder.setPositiveButton(R.string.confirm_option_delete, discardButtonClickListener);
        builder.setNegativeButton(R.string.confirm_option_keep, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Choose an image from Gallery
    void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();

                if (selectedImageUri != null) {
                    mProductImage.setImageURI(selectedImageUri);
                    mImageUri = selectedImageUri;
                }
            }
        }
    }

}
