/*
 * Copyright (C) 2017 Jared Rummler
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

package com.jaredrummler.android.nanodegree.movies.ui.movies;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jaredrummler.android.nanodegree.movies.R;
import com.jaredrummler.android.nanodegree.movies.tmdb.config.MovieSortOrder;
import com.jaredrummler.android.nanodegree.movies.tmdb.model.Movie;
import com.jaredrummler.android.nanodegree.movies.ui.details.DetailsActivity;
import com.jaredrummler.android.nanodegree.movies.ui.movies.adapter.MovieRecyclerViewAdapter;
import com.jaredrummler.android.nanodegree.movies.ui.movies.tasks.FetchMoviesTask;
import com.jaredrummler.android.nanodegree.movies.utils.Prefs;
import com.jaredrummler.android.nanodegree.movies.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MoviesActivity extends AppCompatActivity implements MoviesView {

    private static final String TAG = "MainActivity";

    private static final String OUTSTATE_MOVIES = "nanodegree.state.MOVIES";

    private Prefs mPrefs;

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private ViewGroup mErrorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Find views
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_movies);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_movies_progress);
        mErrorLayout = (ConstraintLayout) findViewById(R.id.error_layout);
        // Get preferences for sort order
        mPrefs = Prefs.with(this);
        // Load the movies
        if (savedInstanceState == null || !savedInstanceState.containsKey(OUTSTATE_MOVIES)) {
            loadMovies();
        } else {
            // No need to fetch movies again.
            List<Movie> movies = savedInstanceState.getParcelableArrayList(OUTSTATE_MOVIES);
            onFetchedMovies(movies);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // Check the sort order preference
        int sortOrderMenuId = Utils.getSortOrderMenuId(mPrefs.getMovieSortOrder());
        menu.findItem(sortOrderMenuId).setChecked(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_now_playing:
            case R.id.action_sort_popular:
            case R.id.action_sort_top_rated:
            case R.id.action_sort_upcoming:
                MovieSortOrder sortOrder = Utils.getMovieSortOrder(item.getItemId());
                mPrefs.saveMovieSortOrder(sortOrder);
                item.setChecked(true);
                loadMovies();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRecyclerView != null) {
            if (mRecyclerView.getAdapter() instanceof MovieRecyclerViewAdapter) {
                MovieRecyclerViewAdapter adapter = (MovieRecyclerViewAdapter) mRecyclerView.getAdapter();
                List<Movie> movies = adapter.getMovies();
                outState.putParcelableArrayList(OUTSTATE_MOVIES, (ArrayList<Movie>) movies);
            }
        }
    }

    @Override
    public void onFetchedMovies(@NonNull List<Movie> movies) {
        mErrorLayout.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mRecyclerView.setHasFixedSize(true);
        int spanCount = getResources().getInteger(R.integer.movies_column_count);
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new MovieRecyclerViewAdapter(movies, this));
    }

    @Override
    public void onMovieClicked(@NonNull Movie movie) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(DetailsActivity.EXTRA_MOVIE, movie);
        startActivity(intent);
    }

    @Override
    public void onFailedToFetchMovies(@Nullable String reason) {
        mErrorLayout.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        TextView tvErrorMessage = (TextView) findViewById(R.id.tv_error_message);
        if (reason != null) {
            tvErrorMessage.setText(reason);
        } else {
            tvErrorMessage.setText(R.string.no_connection_message);
        }
    }

    public void onRefresh(View view) {
        loadMovies();
    }

    private void loadMovies() {
        if (!Utils.isConnectedToInternet(this)) {
            onFailedToFetchMovies(null);
            return;
        }
        mErrorLayout.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        new FetchMoviesTask(this).execute(mPrefs.getMovieSortOrder());
    }

}
