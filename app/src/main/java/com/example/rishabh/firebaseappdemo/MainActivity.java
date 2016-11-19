package com.example.rishabh.firebaseappdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ui.ResultCodes;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import java.util.Arrays;
import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

public class MainActivity extends AppCompatActivity
{
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mRef = database.getReference("chats");
    FirebaseAuth auth;
    FirebaseUser user;
    RecyclerView lvMessages;
    ImageView ivPhoto;
    EditText etField;
    Button bSend;

    public static class Chat {
        private String name;
        private String text;
        private String uid;

        public Chat() {
        }

        public Chat(String name, String uid, String message) {
            this.name = name;
            this.text = message;
            this.uid = uid;
        }

        public String getName() {
            return name;
        }

        public String getUid() {
            return uid;
        }

        public String getText() {
            return text;
        }
    }
    public static class ChatHolder extends RecyclerView.ViewHolder  {
        View mView;

        public ChatHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name) {
            ((TextView) mView.findViewById(R.id.listName)).setText(name);
        }

        public void setText(String text) {
            ((TextView) mView.findViewById(R.id.listText)).setText(text);
        }
    }
    public void createNetworkDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("Network error")
                .setMessage("Please check your network connection before proceeding")
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!isOnline())
                            createNetworkDialog();
                        else
                            checkAuthenticity();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_delete)
                .show();
    }
    public void signOut() {
        if(isOnline() && auth.getCurrentUser() != null) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    log("Signed out user");
                }
            });
        }
    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            log("Online");
            return true;
        } else {
            log("Offline");
            return false;
        }
    }
    final Transformation circleTransform = new Transformation() {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    };
    protected void onActivityResult(int requestCode, int resultCode, Intent data)   {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN)
        {
            if (resultCode == RESULT_OK) {
                log(IdpResponse.fromResultIntent(data).getIdpToken());
                updateViews();
            }
            else if (resultCode == RESULT_CANCELED)
                finish();
            else if (resultCode == ResultCodes.RESULT_NO_NETWORK) {

            }
        }
    }
    void log(String S) {
        Log.d("V_FIREBASE: ", S);
    }
    @Override public boolean onCreateOptionsMenu(Menu menu)  {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item)  {
        switch (item.getItemId())
        {
            case R.id.action_logout:
                signOut();
                finish();
                break;
            case R.id.action_settings:
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(!isOnline())
            createNetworkDialog();
        else
            checkAuthenticity();
    }

    void checkAuthenticity()  {
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        log("User extracted: " + user);

        if (user == null) {
            log("Signing in activity starting");
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setLogo(R.mipmap.ic_launcher)
                            .setProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                    new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                    new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build()))
                            .setIsSmartLockEnabled(false)
                            .build(),
                    RC_SIGN_IN);
        }
        else
            updateViews();
    }

    public void updateViews()
    {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_main);
        lvMessages = (RecyclerView) findViewById(R.id.lvMessages);
        etField = (EditText) findViewById(R.id.etField);
        bSend = (Button) findViewById(R.id.bSend);
        ivPhoto = (ImageView) findViewById(R.id.ivPhoto);

        user = auth.getCurrentUser();
        log("Signing in " + user.getDisplayName());
        log("Photo url: "+user.getPhotoUrl());
        Picasso.with(MainActivity.this).load(user.getPhotoUrl())
                .transform(circleTransform).placeholder(R.drawable.ic_google).into(ivPhoto);
        log("Photo set");

        lvMessages.setHasFixedSize(true);
        lvMessages.setLayoutManager(new LinearLayoutManager(this));
        lvMessages.setAdapter(new FirebaseRecyclerAdapter<Chat, ChatHolder>(Chat.class, R.layout.list_layout, ChatHolder.class, mRef) {
            @Override
            public void populateViewHolder(ChatHolder chatMessageViewHolder, Chat chatMessage, int position) {
                chatMessageViewHolder.setName(chatMessage.getName());
                chatMessageViewHolder.setText(chatMessage.getText());
            }
        });

        etField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateFirebase(bSend);
                    return true;
                }
                return false;
            }
        });
    }
    public void updateFirebase(View v) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etField.getWindowToken(), 0);
        String str = etField.getText().toString();
        if(str== null || str.trim().isEmpty())
            return;
        Chat msg = new Chat(user.getDisplayName(), user.getUid(), str);
        log("Chat created with uid "+user.getUid());
        mRef.push().setValue(msg);
        etField.setText("");
        log("Chat pushed");
    }
}