package oz.moviematch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import oz.moviematch.models.Movie;
import oz.moviematch.models.MoviesDO;
import oz.moviematch.models.ProfilesDO;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DisplayPageActivity extends Activity {
    TextView movieName, movieYear, likePercentage;
    ImageView likeButton, dislikeButton, favoriteButton, moviePoster;
    final String ratingMessage = " liked this movie";
    boolean isLiked = false;
    boolean isNotLiked = false;
    boolean isFavorited = false;
    int percentLiked = 0;
    int movieId = 0;
    Map<String, Boolean> ratings;

    public static final String ARG_OBJECT = "object";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_display);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.omdbapi.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        String id = getIntent().getStringExtra("MOVIE_ID");
        movieId = Integer.parseInt(id);

        OmdbInterface myInterface = retrofit.create(OmdbInterface.class);

        final ArrayList<String> favorites = getFavorites(getBaseContext());

        movieName = findViewById(R.id.movieName);
        movieYear = findViewById(R.id.movieYear);
        moviePoster = findViewById(R.id.poster);
        likeButton = findViewById(R.id.likeButton);
        dislikeButton = findViewById(R.id.dislikeButton);
        likePercentage = findViewById(R.id.likePercentage);
        favoriteButton = findViewById(R.id.favoriteButton);
        myInterface.getMovie("tt" + String.format("%07d", movieId)).enqueue(movieCallback);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //add to movie table
                MoviesDO movie = DBUtils.readMovie("" + movieId);

                ratings = movie.getRatings();
                Boolean rating = ratings.get(DBUtils.getUserId());
                if(rating != null){
                    if(rating){
                        isLiked = true;
                    } else{
                        isNotLiked = true;
                    }
                }
                if(favorites.contains("" + movieId)){
                    isFavorited = true;
                    favoriteButton.setImageResource(R.drawable.ic_star_gold_24dp);
                }

                likeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isLiked = !isLiked;
                        if(isLiked){
                            DBUtils.addRating("" + movieId, true);
                            likeButton.setImageResource(R.drawable.thumbs_up_filled);
                            if(percentLiked != 100){
                                percentLiked++;
                            }
                        } else {
                            likeButton.setImageResource(R.drawable.thumb_up_empty);
                            dislikeButton.setImageResource(R.drawable.thumb_up_empty);
                            isNotLiked = false;
                            if(percentLiked != 0){
                                percentLiked--;
                            }
                        }
                        likePercentage.setText(percentLiked + "% " + ratingMessage);
                    }
                });

                dislikeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isNotLiked = !isNotLiked;
                        if(isNotLiked){
                            DBUtils.addRating("" + movieId, false);
                            dislikeButton.setImageResource(R.drawable.thumbs_up_filled);
                            isLiked = false;
                            likeButton.setImageResource(R.drawable.thumb_up_empty);
                            if(percentLiked != 100){
                                percentLiked--;
                            }
                        } else {
                            dislikeButton.setImageResource(R.drawable.thumb_up_empty);
                            if(percentLiked != 0){
                                percentLiked++;
                            }
                        }
                        likePercentage.setText(percentLiked + "% " + ratingMessage);
                    }
                });



                favoriteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isFavorited = !isFavorited;
                        if(isFavorited){
                            favoriteButton.setImageResource(R.drawable.ic_star_gold_24dp);

                            // Toast
                            Toast toast = Toast.makeText(getApplicationContext(), "Favorite Added", Toast.LENGTH_SHORT);
                            toast.show();

                            // Add to Favorites List
                            addToFavorites("" + movieId, getBaseContext());

                            // DEBUG
                            for(String favorite: favorites){
                                Log.d("Favorites", favorite);
                            }

                        } else {
                            removeFromFavorites("" + movieId, getBaseContext());
                            favoriteButton.setImageResource(R.drawable.ic_star_border_black_24dp);
                        }
                    }
                });
                percentLiked = computeRatings(ratings);
                likePercentage.setText(percentLiked + "% " + ratingMessage);
            }

        }).start();


    }

    Callback<Movie> movieCallback = new Callback<Movie>() {
        @Override
        public void onResponse(Call<Movie> call, Response<Movie> response) {
            if (response.isSuccessful()) {
                Movie movieResponse = response.body();
                // Get All Movie Data
                String posterUrl = movieResponse.getPosterUrl();
                String movieTitle = movieResponse.getTitle();
                String movieRelease = movieResponse.getYear();
                // Set All Movie Data
                movieName.setText(movieTitle);
                movieYear.setText(movieRelease);
                Picasso.get()
                        .load(posterUrl)
                        .placeholder(R.drawable.ic_movie_filter_black_24dp)
                        .error(R.drawable.ic_movie_filter_black_24dp)
                        .resize(600, 700)
                        .into(moviePoster);
            } else {
                Log.d("DisplayPageActivity", "Code: " + response.code() + " Message: " + response.message());
            }
        }

        @Override
        public void onFailure(Call<Movie> call, Throwable t) {
            t.printStackTrace();
        }
    };

    public static void addToFavorites(String value, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        ArrayList<String> list = getFavorites(context);
        list.add(value);
        String json = gson.toJson(list);
        editor.putString("favorites", json);
        editor.apply();
    }

    public static void removeFromFavorites(String value, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        ArrayList<String> list = getFavorites(context);
        list.remove(value);
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("favorites", json);
        editor.apply();
    }

    public static ArrayList<String> getFavorites(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = prefs.getString("favorites", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> list = gson.fromJson(json, type);
        if(list == null){
            list = new ArrayList<String>();
        }
        return list;
    }

    public static int computeRatings(Map<String, Boolean> ratings){
        int x = 0;
        Boolean[] ratingCol = (Boolean[]) ratings.values().toArray();
        for(int i = 0; i < ratingCol.length; i++){
            x += ratingCol[i] ? 1 : 0;
        }

        return (x/ratingCol.length) * 100 ;
    }

    // Call in a thread
    public static String[] getThreeRelaventMovies(int movieId, Map<String, Boolean> ratings){
        String[] movies = new String[3];

        Hashtable<String, Integer> movieReviews = new Hashtable<>();

        for(String name : ratings.keySet()){
            ProfilesDO profile = DBUtils.readProfile(name);
            for(String movie : profile.getRatings().keySet()){
                int reviewCount = movieReviews.containsKey(movie) ? movieReviews.get(movie) + 1 : 1;
                movieReviews.put(movie, reviewCount);
            }
        }




        return movies;
    }


}
