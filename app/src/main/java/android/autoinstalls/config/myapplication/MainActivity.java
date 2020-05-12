package android.autoinstalls.config.myapplication;

import android.autoinstalls.config.myapplication.glide.LifecycleListener;
import android.autoinstalls.config.myapplication.glide.LifecycleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private ImageView imageView;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent intent = new Intent(context, SecondActivity.class);
                context.startActivity(intent);
            }
        });
        imageView = findViewById(R.id.ivHello);
        LifecycleManager.getInstance().get(imageView).addListener(lifecycleListener);
    }

    private LifecycleListener lifecycleListener = new LifecycleListener() {
        @Override
        public void onAttach() {
            Log.d(TAG,"onAttach...");

        }

        @Override
        public void onCreate() {
            Log.d(TAG,"onCreate...");
        }

        @Override
        public void onStart() {
            Log.d(TAG,"onStart...");
        }

        @Override
        public void onResume() {
            Log.d(TAG,"onResume...");
        }

        @Override
        public void onPause() {
            Log.d(TAG,"onPause...");
        }

        @Override
        public void onStop() {
            Log.d(TAG,"onStop...");
        }

        @Override
        public void onDestroy() {
            Log.d(TAG,"onDestroy...");
        }

        @Override
        public void onDetach() {
            Log.d(TAG,"onDetach...");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LifecycleManager.getInstance().get(imageView).removeListener(lifecycleListener);
    }
}
