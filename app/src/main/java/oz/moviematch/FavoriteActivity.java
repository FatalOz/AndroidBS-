package oz.moviematch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.Serializable;
import java.util.List;
import oz.moviematch.models.Movie;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;



public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private MovieRecyclerViewAdapter mAdapter;
    DynamoDBMapper dynamoDBMapper;

    private static EditText mSearchBoxEditText;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        mSearchBoxEditText = (EditText) findViewById(R.id.ma_search_box);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        mRecyclerView = findViewById(R.id.movie_recyclerview);
        mAdapter = new MovieRecyclerViewAdapter(this, DisplayPageActivity.getFavorites(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        AWSMobileClient.getInstance().initialize(this).execute();
        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();

        // Add code to instantiate a AmazonDynamoDBClient
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);

        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();

    }
    Callback<List<Movie>> moviesCallback = new Callback<List<Movie>>() {
        @Override
        public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
            if (response.isSuccessful()) {
                List<Movie> movieResponses = response.body();

                // Populate RecyclerView
                Intent intent = new Intent(getBaseContext(), SearchActivity.class);
                intent.putExtra("MOVIES", (Serializable)  movieResponses);
                getBaseContext().startActivity(intent);
            } else {
                Log.d("DisplayPageActivity", "Code: " + response.code() + " Message: " + response.message());
            }
        }

        @Override
        public void onFailure(Call<List<Movie>> call, Throwable t) {
            t.printStackTrace();
        }
    };
    //search menu item
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if(id==R.id.search){
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://www.omdbapi.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();


            final OmdbInterface myInterface = retrofit.create(OmdbInterface.class);
            Log.d("textBox", mSearchBoxEditText.getText().toString());
            myInterface.getMovies(mSearchBoxEditText.getText().toString()).enqueue(moviesCallback);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}