package in.kay.flixtube.UI.HomeUI;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.gdacciaro.iOSDialog.iOSDialogClickListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.sdsmdg.tastytoast.TastyToast;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import in.kay.flixtube.Adapter.SeriesPlayAdapter;
import in.kay.flixtube.Model.SeriesModel;
import in.kay.flixtube.R;
import in.kay.flixtube.Utils.Helper;

public class DetailActivity extends AppCompatActivity implements PaymentResultListener {

    String imdb, trailer, url, type, title, image, contentType, key;
    TextView tvTitle, tvTime, tvPlot, tvCasting, tvGenre, tvAbout, tvAward, tvAwards, tvCastName, tvImdb, tvSeasons, tvWatch;
    ImageView iv;
    Helper helper;
    RecyclerView rvSeries;
    DatabaseReference rootRef;
    SeriesPlayAdapter seriesPlayAdapter;
    LikeButton likeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        helper = new Helper();
        rootRef = FirebaseDatabase.getInstance().getReference();
        CheckInternet();
    }

    private void CheckInternet() {
        if (helper.isNetwork(this)) {
            InitAll();
        } else {
            Typeface font = Typeface.createFromAsset(this.getAssets(), "Gilroy-ExtraBold.ttf");
            new iOSDialogBuilder(this)
                    .setTitle("Oh shucks!")
                    .setSubtitle("Slow or no internet connection.\nPlease check your internet settings")
                    .setCancelable(false)
                    .setFont(font)
                    .setPositiveListener(getString(R.string.ok), new iOSDialogClickListener() {
                        @Override
                        public void onClick(iOSDialog dialog) {
                            CheckInternet();
                            dialog.dismiss();
                        }
                    })
                    .build().show();
        }
    }

    private void InitAll() {
        rootRef.child("User").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String membership = snapshot.child("Membership").getValue(String.class);
                Initz(membership);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void Initz(final String membership) {
        GetValues();
        InitzViews();
        GetData getData = new GetData();
        getData.execute();
        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayMovie(membership);
            }
        });
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Download();
            }
        });
    }

    private void LoadSeries(JSONObject jsonObject) {
        if (type.equalsIgnoreCase("Series") || type.equalsIgnoreCase("Webseries")) {
            String movieSeason = null;
            try {
                movieSeason = jsonObject.getString("totalSeasons");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            tvSeasons.setText("Total Seasons " + movieSeason);
            tvSeasons.setVisibility(View.VISIBLE);
            findViewById(R.id.ll).setVisibility(View.GONE);
            rvSeries.setVisibility(View.VISIBLE);
            findViewById(R.id.tv_watch).setVisibility(View.VISIBLE);
            findViewById(R.id.ll).setVisibility(View.GONE);
            String key = getIntent().getStringExtra("key");
            FirebaseRecyclerOptions<SeriesModel> options = new FirebaseRecyclerOptions.Builder<SeriesModel>()
                    .setQuery(rootRef.child("Webseries").child(key).child("Source"), SeriesModel.class)
                    .build();
            seriesPlayAdapter = new SeriesPlayAdapter(options, this, image);
            rvSeries.setAdapter(seriesPlayAdapter);
            seriesPlayAdapter.startListening();
        }
    }

    @Override
    public void onPaymentSuccess(String s) {
        rootRef.child("User").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Membership").setValue("VIP").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                TastyToast.makeText(DetailActivity.this, "Welcome to Flixtube VIP club...", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                TastyToast.makeText(DetailActivity.this, "Server down. Error : " + e, TastyToast.LENGTH_LONG, TastyToast.ERROR);
            }
        });
    }

    @Override
    public void onPaymentError(int i, String s) {
        TastyToast.makeText(this, "Payment cancelled.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
    }

    public void back(View view) {
        onBackPressed();
    }

    private class GetData extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.nsv_detail).setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("LOGMSG", "ASYNC CALLED");
            try {
                GetDatafromURL();
            } catch (JSONException | IOException e) {
                Log.d("ErrorIS", "Error is " + e);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TastyToast.makeText(DetailActivity.this, "Something went wrong.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    }
                });

            }
            return null;
        }
    }

    private void GetDatafromURL() throws IOException, JSONException {
        if (type.equalsIgnoreCase("Series") || type.equalsIgnoreCase("Webseries")) {
            String strUrl = "https://api.themoviedb.org/3/find/" + imdb + "?api_key=78f8e2ad04a35e7d8a8117dfef2de601&language=en-US&external_source=imdb_id";
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            if (inputStream == null) {
                TastyToast.makeText(DetailActivity.this, "Something went wrong.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            final JSONObject parentObject = new JSONObject(stringBuilder.toString());
            final JSONArray parentarray = parentObject.getJSONArray("tv_results");
            final JSONObject jsonObject = parentarray.getJSONObject(0);

            final String movieName = title = jsonObject.getString("original_name");
            // final String movieGenre = jsonObject.getString("Genre");
            final double movieImdb = jsonObject.getDouble("vote_average");
            final String movieDate = jsonObject.getString("first_air_date");
            // final String movieTime = jsonObject.getString("Runtime");
            final String moviePoster = image = "https://image.tmdb.org/t/p/w500" + jsonObject.getString("poster_path");
            final String moviePlot = jsonObject.getString("overview");
            //final String movieCast = jsonObject.getString("Actors");
            // final String movieAward = jsonObject.getString("Awards");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UpdateUI(jsonObject, movieName, movieImdb, movieDate, moviePoster, moviePlot);
                }
            });
        } else {
            String strUrl = "https://api.themoviedb.org/3/find/" + imdb + "?api_key=78f8e2ad04a35e7d8a8117dfef2de601&language=en-US&external_source=imdb_id";
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            if (inputStream == null) {
                TastyToast.makeText(DetailActivity.this, "Something went wrong.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            final JSONObject parentObject = new JSONObject(stringBuilder.toString());
            final JSONArray parentarray = parentObject.getJSONArray("movie_results");
            final JSONObject jsonObject = parentarray.getJSONObject(0);

            final String movieName = title = jsonObject.getString("title");
            // final String movieGenre = jsonObject.getString("Genre");
            final double movieImdb = jsonObject.getDouble("vote_average");
            final String movieDate = jsonObject.getString("release_date");
            // final String movieTime = jsonObject.getString("Runtime");
            final String moviePoster = image = "https://image.tmdb.org/t/p/w500" + jsonObject.getString("poster_path");
            final String moviePlot = jsonObject.getString("overview");
            //final String movieCast = jsonObject.getString("Actors");
            // final String movieAward = jsonObject.getString("Awards");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    UpdateUI(jsonObject, movieName, movieImdb, movieDate, moviePoster, moviePlot);
                }
            });
        }
    }

    private void UpdateUI(JSONObject jsonObject, String movieName, double movieImdb, String movieDate, String moviePoster, String moviePlot) {
        tvTitle.setText(movieName);
        tvAbout.setText(moviePlot);
        tvGenre.setText("Release date : "+movieDate);
        tvImdb.setText(movieImdb + "/10");
        //tvCastName.setText(movieCast);
        //tvAwards.setText(movieAward);
        //tvTime.setText(movieTime);
        Picasso.get()
                .load(moviePoster)
                .into(iv);
        LoadSeries(jsonObject);
    }

    private void GetValues() {
        key = getIntent().getStringExtra("key");
        imdb = getIntent().getStringExtra("imdb");
        trailer = getIntent().getStringExtra("trailer");
        type = getIntent().getStringExtra("type");
        url = getIntent().getStringExtra("url");
        contentType = getIntent().getStringExtra("movieType");
    }

    private void InitzViews() {
        Typeface font = Typeface.createFromAsset(this.getAssets(), "Gilroy-ExtraBold.ttf");
        Typeface brandon = Typeface.createFromAsset(this.getAssets(), "Brandon.ttf");
        Typeface typeface = Typeface.createFromAsset(this.getAssets(), "Gilroy-Light.ttf");
        /////////////////////////////////
        likeButton = findViewById(R.id.star_button);
        tvTitle = findViewById(R.id.tv_title);
        tvCasting = findViewById(R.id.tv_casting);
        tvImdb = findViewById(R.id.tv_imdb);
        tvCastName = findViewById(R.id.tv_cast_name);
        tvPlot = findViewById(R.id.tv_plot);
        tvTime = findViewById(R.id.tv_time);
        tvGenre = findViewById(R.id.tv_genre);
        tvWatch = findViewById(R.id.tv_watch);
        tvAbout = findViewById(R.id.tv_about);
        tvSeasons = findViewById(R.id.tv_seasons);
        tvAward = findViewById(R.id.tv_award);
        tvAwards = findViewById(R.id.tv_awards);
        /////////////////////////////////
        iv = findViewById(R.id.iv_cover_img);
        /////////////////////////////////
        rvSeries = findViewById(R.id.rv_series);
        rvSeries.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        /////////////////////////////////
        tvAbout.setTypeface(typeface);
        tvGenre.setTypeface(typeface);
        tvCastName.setTypeface(typeface);
        tvAwards.setTypeface(typeface);
        tvImdb.setTypeface(typeface);
        tvTitle.setTypeface(font);
        tvPlot.setTypeface(brandon);
        tvCasting.setTypeface(brandon);
        tvAward.setTypeface(brandon);
        tvWatch.setTypeface(brandon);
        CheckLike();
        LikeClick();

    }

    private void CheckLike() {
        rootRef.child("User").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Watchlist").child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    likeButton.setLiked(true);
                } else {
                    likeButton.setLiked(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void LikeClick() {
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(final LikeButton likeButton) {
                likeButton.setLiked(true);
                HashMap<String, Object> map = new HashMap<>();
                map.put("image", image);
                map.put("title", title);
                rootRef.child("User").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Watchlist").child(key).setValue(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        TastyToast.makeText(getApplicationContext(), "Added to watchlist successfully", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        TastyToast.makeText(getApplicationContext(), "Something went wrong.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    }
                });
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                likeButton.setLiked(false);
                rootRef.child("User").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Watchlist").child(key).setValue(null);
                TastyToast.makeText(getApplicationContext(), "Removed from watchlist", TastyToast.LENGTH_LONG, TastyToast.INFO);
            }
        });
    }

    public void PlayMovie(String membership) {
        if (contentType.equalsIgnoreCase("Premium")) {
            if (membership.equalsIgnoreCase("VIP")) {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("title", title);
                startActivity(intent);
            } else {
                ShowPopup();
            }
        } else {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("title", title);
            startActivity(intent);
        }
    }

    private void ShowPopup() {
        Typeface font = Typeface.createFromAsset(this.getAssets(), "Gilroy-ExtraBold.ttf");
        new iOSDialogBuilder(DetailActivity.this)
                .setTitle("Buy Premium")
                .setSubtitle("You discovered a premium feature. Streaming a premium content requires VIP account. Press buy to continue")
                .setBoldPositiveLabel(true)
                .setFont(font)
                .setCancelable(false)
                .setPositiveListener(getString(R.string.buy), new iOSDialogClickListener() {
                    @Override
                    public void onClick(iOSDialog dialog) {
                        BuyAccount();
                        dialog.dismiss();

                    }
                })
                .setNegativeListener(getString(R.string.dismiss), new iOSDialogClickListener() {
                    @Override
                    public void onClick(iOSDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .build().show();
    }

    private void BuyAccount() {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_iSJv7N9Z4dJo63");
        final Activity activity = this;
        try {
            JSONObject options = new JSONObject();
            options.put("name", "Flixtube");
            options.put("description", "Purchase premium Flixtube account");
            options.put("currency", "INR");
            String paisee = Integer.toString(Integer.parseInt("200") * 100);
            options.put("amount", paisee);
            checkout.open(activity, options);
        } catch (Exception e) {
            Toast.makeText(this, "Payment error please try again" + e, Toast.LENGTH_SHORT).show();
        }
    }

    public void Download() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            File myDirectory = new File("/Flixtube");
            if(!myDirectory.exists()) {
                myDirectory.mkdirs();
            }
            TastyToast.makeText(this, "Downloading " + title, TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
            helper.DownloadFile(this, title, "Movie", helper.decryptedMsg("Flixtube", url),myDirectory.getAbsolutePath());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                TastyToast.makeText(DetailActivity.this, "Permission successfully granted...", Toast.LENGTH_SHORT, TastyToast.SUCCESS);

            } else {
                TastyToast.makeText(DetailActivity.this, "Please allow us to download..", Toast.LENGTH_SHORT, TastyToast.CONFUSING);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Animatoo.animateSlideRight(this);
    }

    public void TrailerPlay(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("url", trailer);
        intent.putExtra("title", title + " trailer");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckInternet();
    }
}
